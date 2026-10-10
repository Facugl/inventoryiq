"""Lectura de los exports crudos del POS.

Los tres CSV vienen con `;` como separador y en cp1252, y traen entidades HTML
sin desescapar. Eso último no es cosmético: el `;` de `&amp;` coincide con el
separador, así que esas filas quedan con un campo de más y todas las columnas
corridas a partir de ahí. No falla de forma ruidosa — guarda valores en la
columna equivocada —, y por eso la reparación va ANTES de partir la línea.

Ver `docs/InventoryIQ_ETL_Catalogo.md`, secciones 2.1 y 2.2, para la evidencia
sobre los exports reales.
"""

import csv
import io
import re
from collections import Counter
from pathlib import Path

ENCODING_POS = "cp1252"
SEPARADOR_POS = ";"

# Las que aparecen en los exports relevados, más las que podrían aparecer por
# el mismo mecanismo de escapado. Todas contienen un `;`, que es el problema.
ENTIDADES_CONOCIDAS = {
    "&amp;": "&",
    "&quot;": '"',
    "&apos;": "'",
    "&lt;": "<",
    "&gt;": ">",
    "&nbsp;": " ",
}

_ENTIDAD = re.compile(r"&(?:[A-Za-z][A-Za-z0-9]*|#\d+);")


def reparar_entidades(texto: str) -> tuple[str, set[str]]:
    """Desescapa las entidades conocidas y delata las que no lo son.

    Las desconocidas se devuelven en vez de reemplazarse a ciegas: cada una
    corre las columnas de su fila, así que conviene que alguien las mire antes
    de que el ETL decida por su cuenta.
    """
    desconocidas = {e for e in _ENTIDAD.findall(texto) if e not in ENTIDADES_CONOCIDAS}
    for entidad, caracter in ENTIDADES_CONOCIDAS.items():
        texto = texto.replace(entidad, caracter)
    return texto, desconocidas


def leer_csv_pos(ruta: Path) -> tuple[list[dict[str, str]], set[str]]:
    """Filas del CSV como dicts por nombre de columna, ya reparadas.

    Devuelve además las entidades desconocidas encontradas, para que quien
    llame decida si seguir. Las filas cuyo ancho no coincide con el de la
    cabecera NO se descartan acá: se emiten igual y la transformación de cada
    entidad decide, porque sólo ella sabe qué columnas necesita.
    """
    crudo = ruta.read_text(encoding=ENCODING_POS)
    reparado, desconocidas = reparar_entidades(crudo)

    lector = csv.DictReader(io.StringIO(reparado), delimiter=SEPARADOR_POS)
    filas = [{(k or "").strip(): (v or "").strip() for k, v in fila.items()} for fila in lector]
    return filas, desconocidas


def escribir_csv_canonico(ruta: Path, columnas: list[str], filas: list[dict[str, str]]) -> None:
    """Escribe en el formato que ya consumen los adaptadores: coma, UTF-8, LF.

    `newline=""` y `lineterminator="\\n"` evitan que en Windows se escriba
    CRLF: los `data/csv/*.csv` existentes usan LF y el diff quedaría entero.
    """
    ruta.parent.mkdir(parents=True, exist_ok=True)
    with io.open(ruta, "w", encoding="utf-8", newline="") as fh:
        escritor = csv.DictWriter(fh, fieldnames=columnas, lineterminator="\n")
        escritor.writeheader()
        escritor.writerows(filas)


# --- Exports con extensión .xls que no son Excel --------------------------
# El POS llama .xls a dos formatos distintos, y ninguno se abre con xlrd ni
# openpyxl: la consulta de stock es una <table> de HTML, y el listado de
# ventas es SpreadsheetML (el XML de Excel 2003). Se leen con expresiones
# regulares en vez de un parser completo porque la estructura es plana y
# conocida: filas y celdas, sin anidamiento.

_FILA_HTML = re.compile(r"<tr[^>]*>(.*?)</tr>", re.S | re.I)
_CELDA_HTML = re.compile(r"<t[dh][^>]*>(.*?)</t[dh]>", re.S | re.I)
_TAG = re.compile(r"<[^>]+>")


def _texto_celda(html: str) -> str:
    return _TAG.sub("", html).replace("&nbsp;", " ").strip()


def leer_tabla_html(ruta: Path) -> list[dict[str, str]]:
    """Filas de un export HTML disfrazado de .xls, como dicts por columna.

    El reporte trae encabezados sueltos (empresa, título) y una fila de
    cabecera más ancha que los datos, porque el POS une dos filas de header
    en una sola. La cabecera se reconoce como la primera fila que cubre todas
    las columnas de datos con celdas no vacías; las filas que no tienen el
    ancho de los datos son decoración y se ignoran.
    """
    crudo = ruta.read_text(encoding=ENCODING_POS, errors="replace")
    filas = [[_texto_celda(c) for c in _CELDA_HTML.findall(f)] for f in _FILA_HTML.findall(crudo)]
    if not filas:
        return []

    anchos = Counter(len(f) for f in filas if len(f) > 1)
    if not anchos:
        return []
    ancho_datos = anchos.most_common(1)[0][0]

    cabecera = next(
        (f[:ancho_datos] for f in filas
         if len(f) >= ancho_datos and all(f[:ancho_datos])),
        None,
    )
    if cabecera is None:
        return []

    return [
        dict(zip(cabecera, f))
        for f in filas
        if len(f) == ancho_datos and f != cabecera
    ]


_MILES = re.compile(r"[.,](?=\d{3})")


def parsear_entero(valor: str) -> int | None:
    """Entero de un campo del POS, o None si no lo es.

    Los números vienen como "2,700.00000": coma de miles, punto decimal. Se
    devuelve None en vez de 0 para que quien llama distinga "no hay dato" de
    "hay un cero", que es justo lo que pide la decisión 4.1 sobre el stock.
    """
    texto = (valor or "").strip().replace(",", "")
    if not texto:
        return None
    try:
        numero = float(texto)
    except ValueError:
        return None
    return int(numero)

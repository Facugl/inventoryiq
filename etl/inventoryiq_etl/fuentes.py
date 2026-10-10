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

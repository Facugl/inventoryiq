"""Los metadatos que cada export trae en su encabezado y su pie.

Los dos reportes del POS se describen a sí mismos, y eso evita tener que pedirle
a nadie que renombre archivos con una convención:

    Consulta de stock      pie: "... Domingo 6 de Septiembre de 2026 00:43:56"
    Listado de ventas      "Unidad de Negocio: ..."
                           "Período: 01/01/2020 al 11/10/2026"

De ahí salen las tres cosas que el pipeline necesita y que no están en las
filas de datos: **cuándo** se sacó el export, **de qué sucursal** es, y **qué
rango de fechas** cubre.

La fecha sale del pie y no de la fecha de modificación del archivo: copiar o
mover un archivo le cambia el `mtime`, y un snapshot fechado mal no falla, se
atribuye al día equivocado.

El rango importa todavía más. El listado de ventas trae `Cantidad` **agregada
al período**, así que sólo sirve como venta diaria si el período es de un día.
Leerlo del archivo permite rechazar los que no lo son, en vez de registrar tres
meses de ventas como si fueran de un martes.
"""

import re
from dataclasses import dataclass
from datetime import date
from pathlib import Path

ENCODING_POS = "cp1252"

MESES = {
    "enero": 1, "febrero": 2, "marzo": 3, "abril": 4,
    "mayo": 5, "junio": 6, "julio": 7, "agosto": 8,
    "septiembre": 9, "setiembre": 9, "octubre": 10,
    "noviembre": 11, "diciembre": 12,
}

# "Domingo 6 de Septiembre de 2026 00:43:56"
_FECHA_LARGA = re.compile(
    r"(\d{1,2})\s+de\s+([A-Za-zÁÉÍÓÚáéíóú]+)\s+de\s+(\d{4})", re.I
)
# "Período: 01/01/2020 al 11/10/2026"
_PERIODO = re.compile(
    r"Per[ií]odo:.{0,80}?(\d{2}/\d{2}/\d{4}).{0,20}?al.{0,20}?(\d{2}/\d{2}/\d{4})",
    re.I | re.S,
)
_UNIDAD = re.compile(r"Unidad de Negocio:\s*(.{1,120})", re.I)
_TAG = re.compile(r"<[^>]+>")

# Las celdas de encabezado de los dos formatos. Hay que buscar DENTRO de cada
# una: al sacar las marcas, el texto de celdas contiguas queda pegado y
# "Unidad de Negocio: X" se lleva por delante el rótulo siguiente.
# SpreadsheetML usa `<ss:Data>` en el encabezado y `<Data>` a secas en el pie.
_CELDA = re.compile(
    r"<(?:ss:)?Data[^>]*>(.*?)</(?:ss:)?Data>|<t[dh][^>]*>(.*?)</t[dh]>",
    re.S | re.I,
)


def _sin_marcas(texto: str) -> str:
    return _TAG.sub("", texto).replace("&#160;", " ").replace("&nbsp;", " ")


def fecha_larga(texto: str) -> date | None:
    """La primera fecha en castellano largo del texto, o None."""
    encontrada = _FECHA_LARGA.search(texto)
    if not encontrada:
        return None
    mes = MESES.get(encontrada.group(2).lower())
    if mes is None:
        return None
    try:
        return date(int(encontrada.group(3)), mes, int(encontrada.group(1)))
    except ValueError:
        return None


def _fecha_corta(texto: str) -> date | None:
    dia, mes, anio = texto.split("/")
    try:
        return date(int(anio), int(mes), int(dia))
    except ValueError:
        return None


@dataclass(frozen=True)
class Metadatos:
    """Lo que el export dice de sí mismo."""

    generado: date | None = None
    unidad_de_negocio: str = ""
    desde: date | None = None
    hasta: date | None = None

    @property
    def es_de_un_solo_dia(self) -> bool:
        return self.desde is not None and self.desde == self.hasta


def leer(ruta: Path) -> Metadatos:
    """Los metadatos de un export, sea HTML o SpreadsheetML.

    Los dos formatos se leen igual porque lo que interesa está en texto suelto
    dentro de celdas de encabezado, no en la estructura: alcanza con sacar las
    marcas y buscar. Cada campo ausente queda en None en vez de inventarse.
    """
    crudo = ruta.read_text(encoding=ENCODING_POS, errors="replace")
    celdas = [
        _sin_marcas(xml or html).strip()
        for xml, html in _CELDA.findall(crudo)
    ]

    unidad = ""
    desde = hasta = generado = None

    for celda in celdas:
        if not unidad:
            encontrada = _UNIDAD.search(celda)
            if encontrada:
                unidad = encontrada.group(1).strip()
        if desde is None:
            periodo = _PERIODO.search(celda)
            if periodo:
                desde = _fecha_corta(periodo.group(1))
                hasta = _fecha_corta(periodo.group(2))
        if generado is None:
            generado = fecha_larga(celda)

    return Metadatos(
        generado=generado, unidad_de_negocio=unidad, desde=desde, hasta=hasta
    )

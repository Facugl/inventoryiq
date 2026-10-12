"""Los exports que se acumulan día a día.

El catálogo (productos, categorías, proveedores) se saca una vez y se
reemplaza. El historial no: cada export de stock es **un día más** de la serie,
y cada listado de ventas de un día es **ese día**. Perder uno es perderlo para
siempre — el POS no exporta stock retroactivo.

Por eso los dos van juntos a una carpeta aparte, `data/real-export/diarios/`,
y el ETL procesa todo lo que encuentre ahí sin pedirle a nadie que renombre
nada: cada archivo dice de qué fecha es y de qué sucursal (ver `encabezado`).

Los dos formatos se distinguen por su contenido, que es lo único confiable:
el POS llama `.xls` a los dos, y uno es HTML y el otro XML.
"""

from dataclasses import dataclass
from enum import Enum
from pathlib import Path

from . import encabezado

CARPETA = "diarios"


class Tipo(Enum):
    STOCK = "stock"
    VENTAS = "ventas"
    DESCONOCIDO = "desconocido"


@dataclass(frozen=True)
class Export:
    ruta: Path
    tipo: Tipo
    metadatos: encabezado.Metadatos

    @property
    def nombre(self) -> str:
        return self.ruta.name


def _tipo_de(ruta: Path) -> Tipo:
    """Qué reporte es, mirando el contenido y no la extensión ni el nombre."""
    inicio = ruta.read_text(encoding=encabezado.ENCODING_POS, errors="replace")[:4000]
    if "<?xml" in inicio or "urn:schemas-microsoft-com:office:spreadsheet" in inicio:
        return Tipo.VENTAS
    if "<table" in inicio.lower():
        return Tipo.STOCK
    return Tipo.DESCONOCIDO


def encontrar(carpeta: Path) -> list[Export]:
    """Todos los exports de la carpeta, clasificados y con sus metadatos.

    Ordenados por nombre para que dos corridas sobre los mismos archivos
    produzcan exactamente el mismo resultado.
    """
    if not carpeta.is_dir():
        return []

    # La carpeta de exports y, si existe, su subcarpeta de diarios. Así el
    # export que ya estaba en la raíz sigue contando como un día más, y los
    # nuevos se van dejando en `diarios/` sin tener que mover nada.
    rutas = [p for p in carpeta.iterdir() if p.is_file()]
    sub = carpeta / CARPETA
    if sub.is_dir():
        rutas += [p for p in sub.iterdir() if p.is_file()]

    encontrados = []
    for ruta in sorted(rutas, key=lambda p: (p.parent.name, p.name)):
        tipo = _tipo_de(ruta)
        if tipo is Tipo.DESCONOCIDO:
            continue  # CSV del catálogo, notas sueltas, lo que sea
        encontrados.append(Export(ruta, tipo, encabezado.leer(ruta)))
    return encontrados


def de_tipo(exports: list[Export], tipo: Tipo) -> list[Export]:
    return [e for e in exports if e.tipo is tipo]

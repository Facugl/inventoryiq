"""Punto de entrada: python -m inventoryiq_etl <entidad>"""

import argparse
import sys
from pathlib import Path

from . import categorias

# Cada entidad declara su archivo de origen y su archivo de destino. A medida
# que se agreguen proveedores, inventario y productos, se suman acá.
ENTIDADES = {
    "categorias": (categorias.convertir, "categorias_xrp.csv", "categorias.csv"),
}

RAIZ = Path(__file__).resolve().parents[2]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="inventoryiq-etl",
        description="Convierte los exports del POS al formato de data/csv/.",
    )
    parser.add_argument("entidad", choices=sorted(ENTIDADES), help="qué convertir")
    parser.add_argument("--desde", type=Path, default=RAIZ / "data" / "real-export",
                        help="carpeta con los exports crudos del POS")
    parser.add_argument("--hasta", type=Path, default=RAIZ / "data" / "csv",
                        help="carpeta de destino, en el formato que lee el backend")
    parser.add_argument("--dry-run", action="store_true",
                        help="informa qué haría, sin escribir el destino")
    args = parser.parse_args(argv)

    convertir, nombre_origen, nombre_destino = ENTIDADES[args.entidad]
    origen = args.desde / nombre_origen
    if not origen.is_file():
        print(f"No existe el export de origen: {origen}", file=sys.stderr)
        return 2

    destino = args.hasta / nombre_destino
    if args.dry_run:
        # Se escribe igual, pero a un descarte, para que el reporte sea el real
        # y no una estimación: sobreescribir data/csv sin querer es caro.
        import tempfile

        destino = Path(tempfile.mkdtemp()) / nombre_destino

    reporte = convertir(origen, destino)
    print(reporte.resumen())
    print(f"{'(dry-run) ' if args.dry_run else ''}destino: {destino}")

    # Los descartes no son un fallo del proceso: son el resultado, y por eso
    # salen por el reporte. El código de salida distinto de cero se reserva
    # para no haber podido producir nada.
    return 0 if reporte.emitidas else 1

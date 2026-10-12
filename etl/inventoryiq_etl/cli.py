"""Punto de entrada: python -m inventoryiq_etl <comando>"""

import argparse
import sys
import tempfile
from datetime import date
from pathlib import Path

from . import catalogo

RAIZ = Path(__file__).resolve().parents[2]
EXPORT_POS = RAIZ / "data" / "real-export"
DATASET_SIMULADO = RAIZ / "data" / "csv"

# El catálogo real NO va a data/csv: esa carpeta está versionada y el
# repositorio es público. Volcarlo ahí publicaría los costos, los márgenes y
# los proveedores del negocio — exactamente lo que se protegió al ignorar
# data/real-export. Va a una carpeta aparte, ignorada por git, y el backend se
# apunta ahí con INVENTORYIQ_CSV_BASE_PATH.
CATALOGO_REAL = RAIZ / "data" / "csv-real"


def _fecha(valor: str) -> date:
    return date.fromisoformat(valor)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="inventoryiq-etl",
        description="Convierte los exports del POS al formato de CSV que lee el backend.",
    )
    sub = parser.add_subparsers(dest="comando", required=True)

    una = sub.add_parser("entidad", help="convierte una sola entidad")
    una.add_argument("cual", choices=sorted(catalogo.ENTIDADES_MIGRADAS))
    una.add_argument("--desde", type=Path, default=EXPORT_POS)
    una.add_argument("--hasta", type=Path, default=None,
                     help="por defecto, una carpeta temporal: una entidad suelta no "
                          "produce un catálogo utilizable")

    todo = sub.add_parser("migrar", help="genera el catálogo completo y verifica que cierre")
    todo.add_argument("--desde", type=Path, default=EXPORT_POS)
    todo.add_argument("--hasta", type=Path, default=CATALOGO_REAL)
    todo.add_argument("--simulados", type=Path, default=DATASET_SIMULADO,
                      help="de dónde salen los archivos que todavía no tienen fuente en el POS")
    todo.add_argument("--fecha-snapshot", type=_fecha, default=None,
                      help="fecha del inventario (ISO). Por defecto, la de modificación del export")

    args = parser.parse_args(argv)

    if args.comando == "entidad":
        convertir, nombre_origen, nombre_destino = catalogo.ENTIDADES_MIGRADAS[args.cual]
        origen = args.desde / nombre_origen
        if not origen.is_file():
            print(f"No existe el export de origen: {origen}", file=sys.stderr)
            return 2

        hasta = args.hasta or Path(tempfile.mkdtemp())
        reporte = convertir(origen, hasta / nombre_destino)
        print(reporte.resumen())
        print(f"destino: {hasta / nombre_destino}")
        return 0 if reporte.emitidas else 1

    if not args.desde.is_dir():
        print(f"No existe la carpeta de exports: {args.desde}", file=sys.stderr)
        return 2

    resultado = catalogo.migrar(args.desde, args.hasta, args.simulados, args.fecha_snapshot)
    print(resultado.resumen())
    print(f"\ndestino: {resultado.destino}")

    if resultado.sirve and not resultado.alcanza_para_calcular:
        print()
        print("El catálogo cierra, pero todavía no hay con qué calcular: el")
        print("backend arrancaría y las pantallas saldrían vacías. Hace falta")
        print("acumular más días de stock y de ventas (ver etl/README.md).")
    elif resultado.sirve:
        print("\nPara levantar el backend contra este catálogo:")
        print(f"  INVENTORYIQ_CSV_BASE_PATH={resultado.destino}")
    else:
        print("\nNo apuntes el backend a este catálogo: no arrancaría.")

    # Falla cuando el catálogo no sirve. Es la razón de ser del comando: que
    # el problema aparezca ahora y no al arrancar la aplicación.
    return 0 if resultado.sirve else 1

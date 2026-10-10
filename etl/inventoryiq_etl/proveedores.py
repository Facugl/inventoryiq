"""Mantenimiento_de_Proveedores_.csv -> proveedores.csv.

Mapeo y decisiones en `docs/InventoryIQ_ETL_Catalogo.md`, secciones 3.2 y 4.2.
"""

from pathlib import Path

from . import decisiones
from .fuentes import escribir_csv_canonico, leer_csv_pos
from .reporte import Reporte

COLUMNAS_DESTINO = [
    "proveedor_id",
    "razon_social",
    "lead_time_promedio_dias",
    "condicion_pago",
    "activo",
]

COLUMNA_ID = "Id"
COLUMNA_RAZON_SOCIAL = "Razón Social"
COLUMNAS_ORIGEN = {COLUMNA_ID, COLUMNA_RAZON_SOCIAL}


def _fila_destino(proveedor_id: str, razon_social: str) -> dict[str, str]:
    return {
        "proveedor_id": proveedor_id,
        "razon_social": razon_social,
        "lead_time_promedio_dias": decisiones.LEAD_TIME_DIAS,
        "condicion_pago": decisiones.CONDICION_PAGO_SIN_DATO,
        "activo": decisiones.ACTIVO,
    }


def transformar(filas: list[dict[str, str]]) -> tuple[list[dict[str, str]], Reporte]:
    reporte = Reporte(entidad="proveedores", leidas=len(filas))

    destino: list[dict[str, str]] = []
    ids_vistos: set[str] = set()
    sin_asignar_ya_existe = False

    for numero, fila in enumerate(filas, start=1):
        faltantes = COLUMNAS_ORIGEN - fila.keys()
        if faltantes:
            reporte.descartar(numero, "faltan columnas", ", ".join(sorted(faltantes)))
            continue

        proveedor_id = fila[COLUMNA_ID].strip()
        razon_social = fila[COLUMNA_RAZON_SOCIAL].strip()

        if not proveedor_id.isdigit():
            reporte.descartar(numero, "Id no es un entero", repr(proveedor_id))
            continue
        if not razon_social:
            reporte.descartar(numero, "razón social vacía", f"Id={proveedor_id}")
            continue
        if proveedor_id in ids_vistos:
            reporte.descartar(numero, "Id duplicado", proveedor_id)
            continue

        # El id 0 está reservado para el proveedor de la decisión 4.2. Hoy los
        # registrados van de 1 a 62, pero si el POS llegara a emitir un 0 el
        # choque sería silencioso: los productos sin proveedor terminarían
        # apuntando a un proveedor real.
        if proveedor_id == decisiones.PROVEEDOR_SIN_ASIGNAR_ID:
            reporte.descartar(
                numero,
                "Id 0 está reservado para el proveedor de productos sin asignar",
                razon_social,
            )
            continue

        if razon_social == decisiones.PROVEEDOR_SIN_ASIGNAR_NOMBRE:
            # El POS ya lo trae como proveedor registrado: se respeta el suyo
            # en vez de agregar un sintético que lo duplicaría.
            sin_asignar_ya_existe = True

        ids_vistos.add(proveedor_id)
        destino.append(_fila_destino(proveedor_id, razon_social))

    if not sin_asignar_ya_existe:
        destino.insert(
            0,
            _fila_destino(
                decisiones.PROVEEDOR_SIN_ASIGNAR_ID,
                decisiones.PROVEEDOR_SIN_ASIGNAR_NOMBRE,
            ),
        )
        reporte.ajustar(
            0,
            "se agrega el proveedor para los productos que no declaran uno",
            f"id={decisiones.PROVEEDOR_SIN_ASIGNAR_ID}, "
            f"razón social={decisiones.PROVEEDOR_SIN_ASIGNAR_NOMBRE!r}",
        )

    reporte.emitidas = len(destino)
    return destino, reporte


def convertir(origen: Path, destino: Path) -> Reporte:
    filas, entidades_desconocidas = leer_csv_pos(origen)
    destino_filas, reporte = transformar(filas)

    for entidad in sorted(entidades_desconocidas):
        reporte.ajustar(0, "entidad HTML desconocida, no se desescapó", entidad)

    escribir_csv_canonico(destino, COLUMNAS_DESTINO, destino_filas)
    return reporte

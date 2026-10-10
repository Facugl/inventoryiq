"""Consulta_de_stock_xrp.xls -> inventario.csv.

Mapeo y decisiones en `docs/InventoryIQ_ETL_Catalogo.md`, secciones 3.4 y 4.1.

El origen es HTML con extensión .xls (ver `fuentes.leer_tabla_html`).
"""

from datetime import date, datetime
from pathlib import Path

from . import decisiones
from .fuentes import escribir_csv_canonico, leer_tabla_html, parsear_entero
from .reporte import Reporte

COLUMNAS_DESTINO = [
    "inventario_id",
    "fecha_snapshot",
    "producto_id",
    "sucursal_id",
    "stock_actual",
    "stock_en_transito",
]

COLUMNA_UNIDAD = "Unidad de Negocio"
COLUMNA_BARRA = "Código Barra"
COLUMNA_STOCK = "Stk.(U)"
COLUMNAS_ORIGEN = {COLUMNA_UNIDAD, COLUMNA_BARRA, COLUMNA_STOCK}


def transformar(
    filas: list[dict[str, str]],
    fecha_snapshot: date,
    productos_validos: set[str] | None = None,
) -> tuple[list[dict[str, str]], Reporte]:
    """Convierte las filas de stock del POS a snapshots de inventario.

    `productos_validos` permite descartar las filas cuyo producto no exista en
    el catálogo: el backend carga productos e inventario de forma todo-o-nada,
    y una referencia colgada lo haría fallar al arrancar. Mientras productos
    siga bloqueado se pasa None, y entonces la integridad referencial queda
    sin verificar — el reporte lo dice.
    """
    reporte = Reporte(entidad="inventario", leidas=len(filas))
    destino: list[dict[str, str]] = []
    vistos: set[tuple[str, str]] = set()

    for numero, fila in enumerate(filas, start=1):
        faltantes = COLUMNAS_ORIGEN - fila.keys()
        if faltantes:
            reporte.descartar(numero, "faltan columnas", ", ".join(sorted(faltantes)))
            continue

        producto_id = decisiones.producto_id_de(fila[COLUMNA_BARRA])
        if producto_id is None:
            reporte.descartar(numero, "código de barras vacío o no numérico",
                              repr(fila[COLUMNA_BARRA].strip()))
            continue

        unidad = fila[COLUMNA_UNIDAD].strip()
        sucursal_id = decisiones.SUCURSAL_POR_UNIDAD_DE_NEGOCIO.get(unidad)
        if sucursal_id is None:
            reporte.descartar(numero, "unidad de negocio desconocida", repr(unidad))
            continue

        if productos_validos is not None and producto_id not in productos_validos:
            reporte.descartar(numero, "el producto no está en el catálogo", producto_id)
            continue

        clave = (sucursal_id, producto_id)
        if clave in vistos:
            reporte.descartar(numero, "producto repetido en la misma sucursal",
                              f"producto={producto_id}, sucursal={sucursal_id}")
            continue

        stock = parsear_entero(fila[COLUMNA_STOCK])
        if stock is None:
            # Decisión 4.1: no se completa con 0. Un 0 inventado afirmaría que
            # el producto está agotado, y eso dispara una reposición.
            reporte.descartar(numero, "sin dato de stock",
                              f"producto={producto_id}, sucursal={sucursal_id}")
            continue

        if stock < decisiones.STOCK_MINIMO:
            reporte.ajustar(numero, "stock negativo, se registra en 0",
                            f"producto={producto_id}, sucursal={sucursal_id}, stock={stock}")
            stock = decisiones.STOCK_MINIMO

        vistos.add(clave)
        destino.append({
            "inventario_id": str(len(destino) + 1),
            "fecha_snapshot": fecha_snapshot.isoformat(),
            "producto_id": producto_id,
            "sucursal_id": sucursal_id,
            "stock_actual": str(stock),
            "stock_en_transito": decisiones.STOCK_EN_TRANSITO_SIN_DATO,
        })

    if productos_validos is None:
        reporte.ajustar(0, "integridad referencial sin verificar",
                        "no se recibió el catálogo de productos")

    reporte.emitidas = len(destino)
    return destino, reporte


def convertir(
    origen: Path,
    destino: Path,
    fecha_snapshot: date | None = None,
    productos_validos: set[str] | None = None,
) -> Reporte:
    # El export no lleva su fecha adentro: no hay ninguna en el documento. Se
    # cae a la fecha de modificación del archivo, que es una aproximación, y
    # por eso el reporte la deja asentada en vez de usarla en silencio.
    if fecha_snapshot is None:
        fecha_snapshot = datetime.fromtimestamp(origen.stat().st_mtime).date()
        origen_fecha = "fecha de modificación del archivo"
    else:
        origen_fecha = "indicada por quien ejecuta"

    filas = leer_tabla_html(origen)
    destino_filas, reporte = transformar(filas, fecha_snapshot, productos_validos)
    reporte.ajustar(0, f"fecha_snapshot = {fecha_snapshot.isoformat()}", origen_fecha)

    escribir_csv_canonico(destino, COLUMNAS_DESTINO, destino_filas)
    return reporte

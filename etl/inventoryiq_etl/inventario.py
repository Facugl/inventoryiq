"""Consulta_de_stock_xrp.xls -> inventario.csv.

Mapeo y decisiones en `docs/InventoryIQ_ETL_Catalogo.md`, secciones 3.4 y 4.1.

El origen es HTML con extensión .xls (ver `fuentes.leer_tabla_html`).
"""

from datetime import date
from pathlib import Path

from . import decisiones, encabezado, sucursales as sucursales_pos
from .acumular import acumular, leer_existentes
from .diarios import Export, Tipo, de_tipo, encontrar
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

CLAVE = ("fecha_snapshot", "producto_id", "sucursal_id")

COLUMNA_UNIDAD = "Unidad de Negocio"
COLUMNA_BARRA = "Código Barra"
COLUMNA_STOCK = "Stk.(U)"
COLUMNAS_ORIGEN = {COLUMNA_UNIDAD, COLUMNA_BARRA, COLUMNA_STOCK}


def transformar(
    filas: list[dict[str, str]],
    fecha_snapshot: date,
    sucursal_por_unidad: dict[str, str],
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
        sucursal_id = sucursal_por_unidad.get(unidad)
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
    export: Path | None = None,
) -> Reporte:
    """Acumula en `destino` los snapshots de todos los exports de stock.

    `origen` puede ser un export suelto o la carpeta de exports diarios. El
    archivo no se regenera: cada corrida agrega los días que falten y conserva
    los que ya estaban, porque el stock no se puede volver a pedir hacia atrás
    y porque el conteo manual de la pantalla de Administración escribe en este
    mismo archivo (ver `acumular`).
    """
    # Por defecto, al lado del export. Explícito para que un test no escriba
    # dentro del repositorio: el archivo lleva los nombres reales.
    export = export or (origen if origen.is_dir() else origen.parent)

    if origen.is_dir():
        fuentes = de_tipo(encontrar(origen), Tipo.STOCK)
    else:
        fuentes = [Export(origen, Tipo.STOCK, encabezado.leer(origen))]

    # La correspondencia con las sucursales no está en el código: son los
    # nombres reales del negocio y el repositorio es público. Si todavía no se
    # armó, se deriva por volumen (Sección 5.2) y se deja escrita para que se
    # pueda revisar, en vez de decidirlo en silencio en cada corrida.
    sucursal_por_unidad = sucursales_pos.cargar(export)
    origen_correspondencia = f"leída de {sucursales_pos.ARCHIVO}"
    if sucursal_por_unidad is None and fuentes:
        sucursal_por_unidad = sucursales_pos.derivar_por_volumen(
            [f.get(COLUMNA_UNIDAD, "") for f in leer_tabla_html(fuentes[0].ruta)]
        )
        ruta = sucursales_pos.guardar(export, sucursal_por_unidad)
        origen_correspondencia = f"derivada por volumen y guardada en {ruta.name}"

    reporte = Reporte(entidad="inventario")
    acumuladas: list[dict[str, str]] = []
    fechas: list[str] = []

    for fuente in sorted(fuentes, key=lambda e: e.nombre):
        # La fecha sale del pie del propio export, no del `mtime`: copiar un
        # archivo le cambia la fecha de modificación, y un snapshot atribuido
        # al día equivocado no falla, recomienda mal.
        fecha = fecha_snapshot or fuente.metadatos.generado
        if fecha is None:
            reporte.descartar(0, "el export no dice de qué día es", fuente.nombre)
            continue

        filas = leer_tabla_html(fuente.ruta)
        reporte.leidas += len(filas)
        del_dia, parcial = transformar(
            filas, fecha, sucursal_por_unidad or {}, productos_validos
        )
        reporte.descartes += parcial.descartes
        reporte.ajustes += parcial.ajustes
        acumuladas += del_dia
        fechas.append(fecha.isoformat())

    if fechas:
        reporte.ajustar(0, f"snapshots de {len(set(fechas))} día(s)",
                        ", ".join(sorted(set(fechas))))
    reporte.ajustar(0, "correspondencia de sucursales", origen_correspondencia)

    todas = acumular(
        leer_existentes(destino), acumuladas, CLAVE, "inventario_id", reporte
    )
    reporte.emitidas = len(todas)

    # Un archivo vacío no es lo mismo que un archivo que falta: con cabecera y
    # sin filas, la verificación de integridad lo da por presente y el catálogo
    # parece cerrar. Si no hay ni una fila, mejor que no exista y se reporte.
    if todas:
        escribir_csv_canonico(destino, COLUMNAS_DESTINO, todas)
    return reporte

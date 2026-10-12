"""Listado de ventas de un día -> ventas.csv.

Mapeo y decisiones en `docs/InventoryIQ_ETL_Catalogo.md`, secciones 3.5 y 5.3.

El reporte *Ventas de Productos* trae `Cantidad` y `Venta` por producto,
**agregadas al período que se le pidió**. Exportado con `desde = hasta`, ese
agregado es exactamente la venta de ese día: dato real, sin estimar nada.

Por eso el slice **rechaza** los listados de más de un día en vez de usarlos.
Un total de tres meses no se puede repartir en días sin inventar la
distribución, y repartirlo en partes iguales daría una serie plana —
variabilidad cero, estacionalidad nula— que parece buena y es falsa. El período
se lee del propio archivo (`encabezado`), así que no depende de que nadie
nombre bien nada.
"""

from pathlib import Path

from .acumular import acumular, leer_existentes
from .diarios import Export, Tipo, de_tipo
from .fuentes import escribir_csv_canonico, leer_spreadsheetml, parsear_decimal
from .reporte import Reporte

COLUMNAS_DESTINO = [
    "venta_id",
    "fecha",
    "producto_id",
    "sucursal_id",
    "unidades_vendidas",
    "importe_total",
]

CLAVE = ("fecha", "producto_id", "sucursal_id")

COLUMNA_BARRA = "Barra"
COLUMNA_CANTIDAD = "Cantidad"
COLUMNA_VENTA = "Venta"
COLUMNAS_ORIGEN = {COLUMNA_BARRA, COLUMNA_CANTIDAD, COLUMNA_VENTA}


def transformar(
    exports: list[Export],
    sucursal_por_unidad: dict[str, str],
) -> tuple[list[dict[str, str]], Reporte]:
    reporte = Reporte(entidad="ventas", leidas=len(exports))

    destino: list[dict[str, str]] = []
    vistas: set[tuple[str, str, str]] = set()
    redondeadas = 0
    sin_venta = 0

    for numero, export in enumerate(sorted(exports, key=lambda e: e.nombre), start=1):
        meta = export.metadatos

        if not meta.es_de_un_solo_dia:
            # El motivo entero, porque es el error que más caro sale: un
            # listado de tres meses registrado como un martes infla el ADS de
            # todo el catálogo sin que nada falle.
            rango = (f"{meta.desde} al {meta.hasta}" if meta.desde
                     else "sin período en el archivo")
            reporte.descartar(
                numero,
                "el listado no es de un solo día, no sirve como venta diaria",
                f"{export.nombre}: {rango}",
            )
            continue

        sucursal_id = sucursal_por_unidad.get(meta.unidad_de_negocio)
        if sucursal_id is None:
            reporte.descartar(numero, "unidad de negocio desconocida",
                              f"{export.nombre}: {meta.unidad_de_negocio!r}")
            continue

        filas = leer_spreadsheetml(export.ruta)
        if filas and (faltantes := COLUMNAS_ORIGEN - filas[0].keys()):
            reporte.descartar(numero, "faltan columnas",
                              f"{export.nombre}: {', '.join(sorted(faltantes))}")
            continue

        fecha = meta.desde.isoformat()
        emitidas_de_este = 0

        for fila in filas:
            producto_id = (fila.get(COLUMNA_BARRA) or "").strip()
            if not producto_id.isdigit():
                continue

            unidades = parsear_decimal(fila.get(COLUMNA_CANTIDAD, ""))
            importe = parsear_decimal(fila.get(COLUMNA_VENTA, ""))
            if unidades is None or importe is None:
                reporte.descartar(numero, "cantidad o importe no numéricos",
                                  f"{export.nombre}: producto={producto_id}")
                continue

            # `Sale` rechaza negativos por invariante. Un neto negativo es un
            # día de devoluciones, que no es lo mismo que un día sin venta.
            if unidades <= 0 or importe < 0:
                continue

            # `unidades_vendidas` lo parsea `parseInt`: los productos que se
            # venden por peso traen fracciones.
            enteras = int(unidades.to_integral_value())
            if enteras != unidades:
                redondeadas += 1
            if enteras == 0:
                continue

            clave = (fecha, producto_id, sucursal_id)
            if clave in vistas:
                reporte.descartar(numero, "producto repetido en el mismo listado",
                                  f"{export.nombre}: producto={producto_id}")
                continue
            vistas.add(clave)

            destino.append({
                "venta_id": str(len(destino) + 1),
                "fecha": fecha,
                "producto_id": producto_id,
                "sucursal_id": sucursal_id,
                "unidades_vendidas": str(enteras),
                "importe_total": str(importe),
            })
            emitidas_de_este += 1

        if not emitidas_de_este:
            sin_venta += 1

    if redondeadas:
        reporte.ajustar(0, f"{redondeadas} filas con unidades fraccionarias, se redondean",
                        "productos vendidos por peso; unidades_vendidas es entero")
    if sin_venta:
        reporte.ajustar(0, f"{sin_venta} listados sin ninguna venta utilizable", "")

    reporte.emitidas = len(destino)
    return destino, reporte


def convertir(
    origen: Path,
    destino: Path,
    sucursal_por_unidad: dict[str, str] | None = None,
    productos_validos: set[str] | None = None,
) -> Reporte:
    """`origen` es la carpeta de exports diarios, no un archivo."""
    from . import sucursales as sucursales_pos
    from .diarios import encontrar

    if sucursal_por_unidad is None:
        # `origen` ya es la carpeta de exports; junto a ella vive la
        # correspondencia de sucursales (fuera de git, ver `sucursales`).
        carpeta = origen if origen.is_dir() else origen.parent
        sucursal_por_unidad = sucursales_pos.cargar(carpeta) or {}

    exports = de_tipo(encontrar(origen), Tipo.VENTAS)
    filas, reporte = transformar(exports, sucursal_por_unidad)

    if productos_validos is not None:
        antes = len(filas)
        filas = [f for f in filas if f["producto_id"] in productos_validos]
        if antes != len(filas):
            reporte.ajustar(0, f"{antes - len(filas)} ventas de productos fuera del catálogo",
                            "dejarlas colgaría la integridad referencial")
    else:
        reporte.ajustar(0, "integridad referencial sin verificar",
                        "no se recibió el catálogo de productos")

    todas = acumular(leer_existentes(destino), filas, CLAVE, "venta_id", reporte)
    reporte.emitidas = len(todas)

    # Un archivo vacío no es lo mismo que un archivo que falta: con cabecera y
    # sin filas, la verificación de integridad lo da por presente y el catálogo
    # parece cerrar. Si no hay ni una fila, mejor que no exista y se reporte.
    if todas:
        escribir_csv_canonico(destino, COLUMNAS_DESTINO, todas)
    return reporte

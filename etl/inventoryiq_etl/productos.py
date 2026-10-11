"""Cambio_de_precios_xrp.csv -> productos.csv.

Mapeo y decisiones en `docs/InventoryIQ_ETL_Catalogo.md`, secciones 3.3, 4.3,
4.5 y 4.7.

Es el único slice que necesita más de un export, porque el maestro de precios
no trae ni la categoría ni el id del proveedor:

    Cambio_de_precios_xrp.csv   sku, nombre, precios, proveedor (por nombre)
    Listado*.xls                código de barras -> ruta de categoría
    categorias_xrp.csv          ruta de categoría -> categoria_id
    Mantenimiento_de_Proveedores_.csv   razón social -> proveedor_id

Los tres auxiliares se derivan del mismo export que ya usan los otros slices,
con sus mismas transformaciones, así que los ids coinciden por construcción y
no hace falta que el catálogo esté generado para correr éste.
"""

from pathlib import Path

from . import categorias, decisiones, proveedores
from .fuentes import (
    escribir_csv_canonico,
    leer_csv_pos,
    leer_spreadsheetml,
    parsear_decimal,
)
from .reporte import Reporte

COLUMNAS_DESTINO = [
    "producto_id",
    "sku",
    "nombre",
    "categoria_id",
    "proveedor_id",
    "unidad_medida",
    "precio_costo",
    "precio_venta",
    "lead_time_dias",
    "activo",
]

COLUMNA_BARRA = "Cód.Barra"
COLUMNA_NOMBRE = "Denominación"
COLUMNA_PROVEEDOR = "Proveedor"
COLUMNA_PRESENTACION = "Presentación"
COLUMNA_COSTO = "Costo c/IVA"
COLUMNA_VENTA = "Pr.Vta"
COLUMNAS_ORIGEN = {
    COLUMNA_BARRA,
    COLUMNA_NOMBRE,
    COLUMNA_PROVEEDOR,
    COLUMNA_PRESENTACION,
    COLUMNA_COSTO,
    COLUMNA_VENTA,
}

# En el listado de ventas. `Categoría` ya viene como ruta completa
# ("ALMACEN, BEBIDAS"), que es justo la forma de la columna `catego` del
# export de categorías — por eso el cruce es directo y no hay que partir nada.
LISTADO_BARRA = "Barra"
LISTADO_CATEGORIA = "Categoría"

# Los listados de ventas que aportan la categoría. Son varios porque el
# reporte se exporta filtrado por sucursal: cada uno cubre los productos que
# se vendieron en una, y juntos cubren más catálogo que cualquiera solo.
PATRONES_LISTADO = ("Listado*.xls", "Ventas_de_Productos*.xls")

NOMBRE_EXPORT_CATEGORIAS = "categorias_xrp.csv"
NOMBRE_EXPORT_PROVEEDORES = "Mantenimiento_de_Proveedores_.csv"


def categoria_por_barra(
    filas_listado: list[dict[str, str]],
    categoria_id_por_ruta: dict[str, str],
) -> tuple[dict[str, str], set[str]]:
    """Código de barras -> `categoria_id`, y las rutas que no resolvieron.

    Las rutas sin correspondencia se devuelven en vez de ignorarse: una ruta
    que el export de categorías no conoce significa que los dos exports se
    sacaron en momentos distintos, y conviene que se vea.
    """
    por_barra: dict[str, str] = {}
    sin_resolver: set[str] = set()

    for fila in filas_listado:
        barra = (fila.get(LISTADO_BARRA) or "").strip()
        ruta = (fila.get(LISTADO_CATEGORIA) or "").strip()
        if not barra or not ruta:
            continue

        categoria_id = categoria_id_por_ruta.get(ruta)
        if categoria_id is None:
            sin_resolver.add(ruta)
            continue

        # El primero gana: si dos listados discrepan, la categoría es un
        # atributo del producto y no de la sucursal, así que la diferencia
        # sería un dato sucio y no una distinción real.
        por_barra.setdefault(barra, categoria_id)

    return por_barra, sin_resolver


def transformar(
    filas: list[dict[str, str]],
    categorias_de_producto: dict[str, str],
    proveedor_id_por_nombre: dict[str, str],
    categorias_validas: set[str],
) -> tuple[list[dict[str, str]], Reporte]:
    reporte = Reporte(entidad="productos", leidas=len(filas))

    sin_categoria_disponible = (
        decisiones.CATEGORIA_SIN_ASIGNAR_ID not in categorias_validas
    )
    if sin_categoria_disponible:
        # Sin ella no hay dónde poner a los productos sin categoría, y
        # emitirlos igual dejaría una referencia colgada que haría fallar el
        # arranque del backend. Se descartan, y el motivo queda dicho una vez.
        reporte.ajustar(
            0,
            "el export de categorías no trae la categoría para los productos sin asignar",
            f"categoria_id={decisiones.CATEGORIA_SIN_ASIGNAR_ID} "
            f"({decisiones.CATEGORIA_SIN_ASIGNAR_NOMBRE})",
        )

    destino: list[dict[str, str]] = []
    ids_vistos: set[str] = set()
    sin_categoria = 0
    sin_unidad = 0

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
        if producto_id in ids_vistos:
            reporte.descartar(numero, "código de barras duplicado", producto_id)
            continue

        nombre = fila[COLUMNA_NOMBRE].strip()
        if not nombre:
            reporte.descartar(numero, "denominación vacía", producto_id)
            continue

        nombre_proveedor = fila[COLUMNA_PROVEEDOR].strip()
        proveedor_id = proveedor_id_por_nombre.get(nombre_proveedor)
        if proveedor_id is None:
            # No se cae al proveedor sin asignar: eso le cambiaría el lead
            # time al producto en silencio, y de ahí sale el punto de pedido.
            reporte.descartar(numero, "proveedor sin correspondencia",
                              f"producto={producto_id}, proveedor={nombre_proveedor!r}")
            continue

        categoria_id = categorias_de_producto.get(producto_id)
        if categoria_id is None:
            if sin_categoria_disponible:
                reporte.descartar(numero, "sin categoría y sin dónde ubicarlo", producto_id)
                continue
            categoria_id = decisiones.CATEGORIA_SIN_ASIGNAR_ID
            sin_categoria += 1
        elif categoria_id not in categorias_validas:
            reporte.descartar(numero, "la categoría no está en el catálogo",
                              f"producto={producto_id}, categoria={categoria_id}")
            continue

        costo = parsear_decimal(fila[COLUMNA_COSTO])
        venta = parsear_decimal(fila[COLUMNA_VENTA])
        if costo is None or venta is None:
            # El backend parsea estos campos con BigDecimal y no tolera un
            # vacío: con uno solo, no arranca. Y un 0 inventado distorsiona
            # el capital inmovilizado y la clasificación ABC.
            reporte.descartar(numero, "precio ausente o no numérico",
                              f"producto={producto_id}, "
                              f"costo={fila[COLUMNA_COSTO].strip()!r}, "
                              f"venta={fila[COLUMNA_VENTA].strip()!r}")
            continue

        unidad = decisiones.unidad_medida_de(fila[COLUMNA_PRESENTACION])
        if unidad is None:
            unidad = decisiones.UNIDAD_MEDIDA_DEFAULT
            sin_unidad += 1

        ids_vistos.add(producto_id)
        destino.append({
            "producto_id": producto_id,
            "sku": producto_id,
            "nombre": nombre,
            "categoria_id": categoria_id,
            "proveedor_id": proveedor_id,
            "unidad_medida": unidad,
            "precio_costo": str(costo),
            "precio_venta": str(venta),
            "lead_time_dias": decisiones.LEAD_TIME_DIAS,
            "activo": decisiones.ACTIVO,
        })

    # Agregados, no una incidencia por fila: son miles y el detalle por
    # producto no agrega nada que el número no diga.
    if sin_categoria:
        reporte.ajustar(
            0,
            f"{sin_categoria} productos sin categoría en los listados, "
            f"van a {decisiones.CATEGORIA_SIN_ASIGNAR_NOMBRE!r}",
            f"categoria_id={decisiones.CATEGORIA_SIN_ASIGNAR_ID}, decisión 4.7",
        )
    if sin_unidad:
        reporte.ajustar(
            0,
            f"{sin_unidad} productos sin unidad reconocible en Presentación",
            f"se registran como {decisiones.UNIDAD_MEDIDA_DEFAULT}",
        )

    reporte.emitidas = len(destino)
    return destino, reporte


def _auxiliares(export: Path, listados: list[Path]) -> tuple[dict, dict, set, list]:
    """Las tres tablas de apoyo, derivadas de los mismos exports y transformaciones."""
    avisos: list[tuple[str, str]] = []

    filas_categorias, _ = leer_csv_pos(export / NOMBRE_EXPORT_CATEGORIAS)
    emitidas_categorias, _ = categorias.transformar(filas_categorias)
    categorias_validas = {c["categoria_id"] for c in emitidas_categorias}
    # `catego` es la ruta completa y es única en el export real; `denom` no lo
    # es (38 nombres se repiten entre departamentos), así que no sirve como
    # clave de cruce. Si aun así llegaran dos rutas iguales, gana la primera:
    # el orden del archivo es estable, y resolver al azar movería productos de
    # categoría entre corridas sin que nada lo delate.
    categoria_id_por_ruta: dict[str, str] = {}
    for fila in filas_categorias:
        idcate = (fila.get("idcate") or "").strip()
        ruta = (fila.get("catego") or "").strip()
        if ruta and idcate in categorias_validas:
            categoria_id_por_ruta.setdefault(ruta, idcate)

    filas_proveedores, _ = leer_csv_pos(export / NOMBRE_EXPORT_PROVEEDORES)
    emitidos_proveedores, _ = proveedores.transformar(filas_proveedores)
    proveedor_id_por_nombre = {
        p["razon_social"]: p["proveedor_id"] for p in emitidos_proveedores
    }

    categorias_de_producto: dict[str, str] = {}
    rutas_sin_resolver: set[str] = set()
    for listado in listados:
        por_barra, sin_resolver = categoria_por_barra(
            leer_spreadsheetml(listado), categoria_id_por_ruta
        )
        for barra, categoria_id in por_barra.items():
            categorias_de_producto.setdefault(barra, categoria_id)
        rutas_sin_resolver |= sin_resolver

    if not listados:
        avisos.append((
            "no se encontró ningún listado de ventas con las categorías",
            ", ".join(PATRONES_LISTADO),
        ))
    for ruta in sorted(rutas_sin_resolver):
        avisos.append(("ruta de categoría que el export de categorías no trae", ruta))

    return categorias_de_producto, proveedor_id_por_nombre, categorias_validas, avisos


def listados_de(export: Path) -> list[Path]:
    """Los listados de ventas presentes en la carpeta de exports."""
    encontrados: list[Path] = []
    for patron in PATRONES_LISTADO:
        encontrados += sorted(export.glob(patron))
    return encontrados


def convertir(
    origen: Path,
    destino: Path,
    listados: list[Path] | None = None,
) -> Reporte:
    export = origen.parent
    if listados is None:
        listados = listados_de(export)

    filas, entidades_desconocidas = leer_csv_pos(origen)
    categorias_de_producto, proveedor_id_por_nombre, categorias_validas, avisos = (
        _auxiliares(export, listados)
    )

    destino_filas, reporte = transformar(
        filas, categorias_de_producto, proveedor_id_por_nombre, categorias_validas
    )

    for listado in listados:
        reporte.ajustar(0, "categorías tomadas del listado de ventas", listado.name)
    for motivo, detalle in avisos:
        reporte.ajustar(0, motivo, detalle)
    for entidad in sorted(entidades_desconocidas):
        reporte.ajustar(0, "entidad HTML desconocida, no se desescapó", entidad)

    escribir_csv_canonico(destino, COLUMNAS_DESTINO, destino_filas)
    return reporte

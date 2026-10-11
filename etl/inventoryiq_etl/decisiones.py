"""Valores que el POS no exporta y alguien tuvo que decidir.

Están acá, juntos y nombrados, porque no son detalles de implementación: son
decisiones de negocio documentadas en `docs/InventoryIQ_ETL_Catalogo.md`,
sección 4. Varias las usa más de una entidad, y cambiarlas mueve números que
el usuario ve (el punto de pedido, el umbral de sobrestock).
"""

# --- Decisión 4.3 — Lead time -------------------------------------------
# El POS no lo registra, ni por producto ni por proveedor. Un único valor para
# todas las filas, corregible por proveedor desde la pantalla de Proveedores.
# Alimenta directamente el punto de pedido: bajo genera quiebres, alto infla
# las sugerencias de compra.
LEAD_TIME_DIAS = "3"

# --- Decisión 4.4 — Parámetros de categoría ------------------------------
# Tampoco los exporta el POS. Se aplica a todas el valor más frecuente de los
# datos simulados, ajustable por categoría desde Administración.
UMBRAL_SOBRESTOCK_DIAS = "20.0"
COBERTURA_EXTRA_DIAS = "4.0"

# --- Decisión 4.2 — Proveedor para los productos que no declaran uno ------
# El 36% del catálogo trae el literal "Sin Proveedor". Se crea como proveedor
# real para que esos productos entren al sistema y se puedan reponer.
#
# El id es 0 y el nombre es exactamente el literal del POS, y ninguna de las
# dos cosas es arbitraria:
#   - El POS usa Id.Prov.=0 exclusivamente para "Sin Proveedor" (verificado
#     sobre Ventas_de_Productos.xls), y 0 no está tomado por ningún proveedor
#     registrado, que van de 1 a 62.
#   - El export de precios no trae id de proveedor, sólo el nombre como texto,
#     así que productos va a resolver el proveedor por nombre. Conservar el
#     literal hace que ese cruce no necesite ningún caso especial.
PROVEEDOR_SIN_ASIGNAR_ID = "0"
PROVEEDOR_SIN_ASIGNAR_NOMBRE = "Sin Proveedor"

# --- Decisión 4.7 — Productos sin categoría -------------------------------
# El listado de ventas trae la categoría de los productos que se vendieron
# alguna vez, no de todo el catálogo: quedan 3.862 productos sin ella, de los
# cuales sólo 599 tienen stock.
#
# No se inventa una categoría: se usa la que el POS ya trae. `categorias_xrp`
# declara `idcate=0, No Categorizado`, y el slice de categorías la emite como
# una raíz más. Es el mismo caso que el proveedor de la decisión 4.2 — el
# centinela existe en el origen, no lo agrega el ETL.
#
# Consecuencia aceptada: esos productos comparten umbral de sobrestock y
# cobertura extra, como cualquier categoría (decisión 4.4), y se corrigen
# desde Administración. Los 599 con stock entran al sistema y se pueden
# reponer; el resto queda inerte, sin stock ni ventas que disparen nada.
CATEGORIA_SIN_ASIGNAR_ID = "0"
CATEGORIA_SIN_ASIGNAR_NOMBRE = "No Categorizado"


# --- Unidad de medida -----------------------------------------------------
# El POS no la declara como tal: viene dentro de `Presentación`, al final y en
# su propia nomenclatura ("1x1 Un", "1x Kgr", "1x700 ml"). Se traduce al
# vocabulario del dataset simulado (UN/KG), que es lo que el backend arrastra
# en `Product.unitOfMeasure`.
#
# Ese campo hoy no alimenta ningún cálculo: el dominio lo transporta y nadie
# lo lee. Por eso un valor desconocido no descarta el producto — se cae al
# default y el reporte lo deja asentado.
UNIDAD_POR_PRESENTACION = {
    "un": "UN",
    "kgr": "KG",
    "kg": "KG",
    "lts": "LT",
    "lt": "LT",
    "ml": "ML",
    "grs": "GR",
    "gr": "GR",
}
UNIDAD_MEDIDA_DEFAULT = "UN"


def unidad_medida_de(presentacion: str) -> str | None:
    """La unidad que declara una `Presentación` del POS, o None si no la trae."""
    tokens = (presentacion or "").strip().split()
    if not tokens:
        return None
    return UNIDAD_POR_PRESENTACION.get(tokens[-1].lower())


# --- Decisión 4.5 — Estado de los productos y proveedores -----------------
ACTIVO = "True"

# El POS no exporta condición de pago para los proveedores. Se deja vacío en
# vez de inventar un valor: un "Contado" falso cambiaría cómo se lee la
# pantalla de Proveedores.
CONDICION_PAGO_SIN_DATO = ""


# --- Identidad del producto ----------------------------------------------
# producto_id sale del código de barras, y no de un contador del ETL ni de los
# códigos internos del POS. Los motivos, en orden de peso:
#
#   1. Tiene que ser ESTABLE entre corridas. Las recomendaciones viven en
#      Postgres y guardan el productId: si una re-corrida del ETL reasignara
#      los ids, cada recomendación pasaría a apuntar a otro producto, en
#      silencio. Un contador secuencial depende del orden del archivo y del
#      catálogo del día; el código de barras no.
#   2. Es la única clave que comparten los dos exports. `Cod.Int` coincide
#      entre stock y precios (10.178 de 10.178 verificados), pero tiene 98
#      duplicados en precios, así que no identifica. El `Id.` de precios sí es
#      casi único, pero no está en el export de stock.
#   3. Es único en precios: 10.325 valores, 10.325 distintos.
#
# Consecuencia aceptada: producto_id y sku terminan siendo el mismo número.
# Es redundante, pero preferible a un id inventado que se pueda mover.


def producto_id_de(codigo_barra: str) -> str | None:
    """El producto_id que le corresponde a un código de barras, o None."""
    codigo = (codigo_barra or "").strip()
    return codigo if codigo.isdigit() else None


# --- Sucursales -----------------------------------------------------------
# El export de stock nombra las sucursales en `Unidad de Negocio`; el backend
# las identifica por el id de `data/csv/sucursales.csv`.
#
# La correspondencia se infiere por VOLUMEN: la unidad con 10.148 filas se toma
# como la principal. No se puede verificar contra los nombres, porque
# `sucursales.csv` tiene los simulados (Centro, Norte, Sur) y no los reales del
# POS.
#
# Decisión del usuario (2026-10-11, Sección 5.2 del doc): queda así. Los
# nombres simulados se conservan —tienen la ventaja de no publicar los nombres
# reales del negocio en un repositorio público— y se arrastra `sucursales.csv`
# tal cual al catálogo real, que vive en `data/csv-real/` y no en `data/csv/`.
#
# Lo que sigue en pie es la consecuencia si el mapeo estuviera invertido: un
# stock cargado en la sucursal equivocada no falla, simplemente recomienda mal.
SUCURSAL_POR_UNIDAD_DE_NEGOCIO = {
    "Administración Central Alem": "1",
    "Sucursal Santa Cruz": "2",
}

# --- Decisión 4.1 — Stock -------------------------------------------------
# Un stock negativo en el POS es casi siempre un desfasaje contable, no
# mercadería real: para decidir una reposición, 0 es lo honesto. Las filas sin
# valor NO se completan con 0, porque eso afirmaría que el producto está
# agotado: se omiten.
STOCK_MINIMO = 0

# El POS no informa mercadería en tránsito.
STOCK_EN_TRANSITO_SIN_DATO = "0"

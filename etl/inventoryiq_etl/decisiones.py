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
# OJO: `sucursales.csv` todavía tiene los nombres SIMULADOS (Centro, Norte,
# Sur), no los reales del POS, así que esta correspondencia es una suposición
# por volumen: la unidad con 10.148 filas se toma como la principal. Hay que
# confirmarla antes de volcar el inventario a `data/csv/`: un stock cargado en
# la sucursal equivocada no falla, simplemente recomienda mal.
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

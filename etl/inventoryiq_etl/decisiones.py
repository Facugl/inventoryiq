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

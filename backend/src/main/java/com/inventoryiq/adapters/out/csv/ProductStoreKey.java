package com.inventoryiq.adapters.out.csv;

/**
 * Clave de indexación en memoria por producto+sucursal, compartida por los
 * adaptadores CSV cuyos archivos tienen esa granularidad (ventas.csv,
 * inventario.csv). Es un detalle interno de estos adaptadores, no forma
 * parte de ningún puerto.
 */
record ProductStoreKey(Long productId, Long storeId) {
}

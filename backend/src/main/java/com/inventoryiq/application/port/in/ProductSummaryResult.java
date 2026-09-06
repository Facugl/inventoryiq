package com.inventoryiq.application.port.in;

/** Salida de SearchProductsUseCase — lo mínimo para elegir un producto en un selector. */
public record ProductSummaryResult(Long productId, String sku, String name, Long categoryId) {
}

package com.inventoryiq.adapters.in.rest.dto;

/** Forma JSON pública de un producto en resultados de búsqueda (GET /api/v1/products?q=...). */
public record ProductSummaryResponse(Long productId, String sku, String name, Long categoryId) {
}

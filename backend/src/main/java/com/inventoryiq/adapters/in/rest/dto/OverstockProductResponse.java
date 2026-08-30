package com.inventoryiq.adapters.in.rest.dto;

import java.math.BigDecimal;

/**
 * Forma JSON pública de un producto en sobrestock (GET /api/v1/products/overstock).
 * Deliberadamente distinto de OverstockProductResult (application/port/in),
 * mismo criterio que CriticalProductResponse en el slice anterior.
 */
public record OverstockProductResponse(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		int currentStock,
		double currentDaysOfCoverage,
		BigDecimal immobilizedValue) {
}

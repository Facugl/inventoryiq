package com.inventoryiq.adapters.in.rest.dto;

/** Forma JSON pública del resumen de una sucursal dentro de una corrida de POST /api/v1/product-status/recalculate. */
public record StoreRecalculationSummaryResponse(
		Long storeId,
		int criticalProductsFound,
		int overstockProductsFound,
		int alertsGenerated,
		RecalculateRecommendationsResponse recommendations) {
}

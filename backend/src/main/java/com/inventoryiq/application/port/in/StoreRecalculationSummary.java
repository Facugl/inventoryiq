package com.inventoryiq.application.port.in;

/** Resumen de la recorrida de RecalculateProductStatusUseCase (Sección 9.10) para una sucursal puntual. */
public record StoreRecalculationSummary(
		Long storeId,
		int criticalProductsFound,
		int overstockProductsFound,
		int alertsGenerated,
		RecalculateRecommendationsResult recommendations) {
}

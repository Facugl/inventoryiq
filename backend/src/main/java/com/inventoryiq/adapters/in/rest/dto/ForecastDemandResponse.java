package com.inventoryiq.adapters.in.rest.dto;

import java.util.List;

/** Forma JSON pública de una proyección de demanda (GET /api/v1/products/{productId}/forecast). */
public record ForecastDemandResponse(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Double baseAds,
		List<DemandForecastPeriodResponse> periods) {
}

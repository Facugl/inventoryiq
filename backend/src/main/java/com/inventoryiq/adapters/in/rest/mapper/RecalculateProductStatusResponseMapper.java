package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.RecalculateProductStatusResponse;
import com.inventoryiq.adapters.in.rest.dto.RecalculateRecommendationsResponse;
import com.inventoryiq.adapters.in.rest.dto.StoreRecalculationSummaryResponse;
import com.inventoryiq.application.port.in.RecalculateProductStatusResult;
import com.inventoryiq.application.port.in.StoreRecalculationSummary;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class RecalculateProductStatusResponseMapper {
	private RecalculateProductStatusResponseMapper() {
	}

	public static RecalculateProductStatusResponse toResponse(RecalculateProductStatusResult result) {
		return new RecalculateProductStatusResponse(
				result.storesProcessed(),
				result.perStore().stream().map(RecalculateProductStatusResponseMapper::toResponse).toList());
	}

	private static StoreRecalculationSummaryResponse toResponse(StoreRecalculationSummary summary) {
		return new StoreRecalculationSummaryResponse(
				summary.storeId(),
				summary.criticalProductsFound(),
				summary.overstockProductsFound(),
				summary.alertsGenerated(),
				new RecalculateRecommendationsResponse(
						summary.recommendations().totalGenerated(),
						summary.recommendations().newCount(),
						summary.recommendations().updatedCount(),
						summary.recommendations().autoDiscardedCount()));
	}
}

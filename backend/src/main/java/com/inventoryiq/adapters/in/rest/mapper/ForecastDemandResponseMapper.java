package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.DemandForecastPeriodResponse;
import com.inventoryiq.adapters.in.rest.dto.ForecastDemandResponse;
import com.inventoryiq.application.port.in.DemandForecastPeriod;
import com.inventoryiq.application.port.in.ForecastDemandResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class ForecastDemandResponseMapper {
	private ForecastDemandResponseMapper() {
	}

	public static ForecastDemandResponse toResponse(ForecastDemandResult result) {
		return new ForecastDemandResponse(
				result.productId(),
				result.sku(),
				result.productName(),
				result.storeId(),
				result.baseAds(),
				result.periods().stream().map(ForecastDemandResponseMapper::toResponse).toList());
	}

	private static DemandForecastPeriodResponse toResponse(DemandForecastPeriod period) {
		return new DemandForecastPeriodResponse(
				period.periodStart(),
				period.periodEnd(),
				period.seasonalIndex(),
				period.projectedDailyAds(),
				period.projectedTotalDemand());
	}
}

package com.inventoryiq.adapters.in.rest.dto;

import java.time.LocalDate;

/** Forma JSON pública de un período proyectado dentro de un ForecastDemandResponse. */
public record DemandForecastPeriodResponse(
		LocalDate periodStart,
		LocalDate periodEnd,
		double seasonalIndex,
		double projectedDailyAds,
		int projectedTotalDemand) {
}

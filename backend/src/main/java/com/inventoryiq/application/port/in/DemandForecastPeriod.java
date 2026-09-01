package com.inventoryiq.application.port.in;

import java.time.LocalDate;

/**
 * Un período (semana) dentro del horizonte proyectado por
 * ForecastDemandUseCase — Sección 9.4.
 */
public record DemandForecastPeriod(
		LocalDate periodStart,
		LocalDate periodEnd,
		double seasonalIndex,
		double projectedDailyAds,
		int projectedTotalDemand) {
}

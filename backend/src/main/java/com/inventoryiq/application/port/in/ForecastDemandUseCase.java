package com.inventoryiq.application.port.in;

/** Puerto de entrada — Sección 9.4. Lanza ProductNotFoundException si productId no existe en el catálogo. */
public interface ForecastDemandUseCase {

	ForecastDemandResult execute(ForecastDemandQuery query);
}

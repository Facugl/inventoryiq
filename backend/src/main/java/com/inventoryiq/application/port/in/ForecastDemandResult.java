package com.inventoryiq.application.port.in;

import java.util.List;

/**
 * Resultado de ForecastDemandUseCase (Sección 9.4).
 *
 * baseAds es null y periods es una lista vacía cuando el producto existe
 * pero no tiene historial de ventas suficiente para calcular un ADS base
 * (Sección 4.9) — el recurso existe, simplemente no se puede proyectar.
 * Un productId que no existe en absoluto no llega a este resultado: el
 * caso de uso lanza ProductNotFoundException antes.
 */
public record ForecastDemandResult(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Double baseAds,
		List<DemandForecastPeriod> periods) {
}

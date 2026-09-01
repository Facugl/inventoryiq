package com.inventoryiq.application.port.in;

import java.util.List;

/** Resumen de ejecución de RecalculateProductStatusUseCase (Sección 9.10), una entrada por sucursal procesada. */
public record RecalculateProductStatusResult(
		int storesProcessed,
		List<StoreRecalculationSummary> perStore) {
}

package com.inventoryiq.adapters.in.rest.dto;

import java.util.List;

/** Forma JSON pública del resumen de ejecución de POST /api/v1/product-status/recalculate (Sección 9.10). */
public record RecalculateProductStatusResponse(
		int storesProcessed,
		List<StoreRecalculationSummaryResponse> perStore) {
}

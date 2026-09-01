package com.inventoryiq.adapters.in.rest.dto;

import java.util.List;

/** Forma JSON pública del resumen de una ingesta CSV (POST /api/v1/csv-ingestions). */
public record IngestionSummaryResponse(
		int totalRowsRead,
		int acceptedCount,
		int rejectedCount,
		List<RowRejectionResponse> rejections) {
}

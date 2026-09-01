package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.IngestionSummaryResponse;
import com.inventoryiq.adapters.in.rest.dto.RowRejectionResponse;
import com.inventoryiq.application.port.in.IngestCsvFileResult;
import com.inventoryiq.application.port.in.RowRejection;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class IngestionSummaryResponseMapper {
	private IngestionSummaryResponseMapper() {
	}

	public static IngestionSummaryResponse toResponse(IngestCsvFileResult result) {
		return new IngestionSummaryResponse(
				result.totalRowsRead(),
				result.acceptedCount(),
				result.rejectedCount(),
				result.rejections().stream().map(IngestionSummaryResponseMapper::toResponse).toList());
	}

	private static RowRejectionResponse toResponse(RowRejection rejection) {
		return new RowRejectionResponse(rejection.rowNumber(), rejection.reason());
	}
}

package com.inventoryiq.application.port.in;

import java.util.List;

/** Resumen de ejecución de IngestCsvFileUseCase (Sección 8.13/9.9), cuando el lote sí se pudo aplicar. */
public record IngestCsvFileResult(
		int totalRowsRead,
		int acceptedCount,
		int rejectedCount,
		List<RowRejection> rejections) {
}

package com.inventoryiq.domain.exception;

import java.util.List;
import java.util.Locale;

/**
 * Se lanza cuando, en una ingesta de CSV (Sección 9.9), el porcentaje de
 * filas rechazadas supera el umbral crítico configurable (Sección 7.4).
 * A diferencia de InvalidDomainDataException (un dato puntual inválido,
 * 400), esto representa un lote entero que no se pudo aplicar de forma
 * segura — se traduce a 422 en GlobalExceptionHandler, y no se persiste
 * ninguna fila del lote.
 */
public class CsvIngestionThresholdExceededException extends RuntimeException {

	private final int totalRowsRead;
	private final int rejectedCount;
	private final double rejectionRatePercent;
	private final List<?> rejections;

	public CsvIngestionThresholdExceededException(
			int totalRowsRead, int rejectedCount, double rejectionRatePercent, List<?> rejections) {
		super(String.format(Locale.ROOT,
				"Rejection rate %.2f%% (%d of %d rows) exceeds the critical threshold; the batch was aborted, nothing was persisted",
				rejectionRatePercent, rejectedCount, totalRowsRead));
		this.totalRowsRead = totalRowsRead;
		this.rejectedCount = rejectedCount;
		this.rejectionRatePercent = rejectionRatePercent;
		this.rejections = rejections;
	}

	public int getTotalRowsRead() {
		return totalRowsRead;
	}

	public int getRejectedCount() {
		return rejectedCount;
	}

	public double getRejectionRatePercent() {
		return rejectionRatePercent;
	}

	public List<?> getRejections() {
		return rejections;
	}
}

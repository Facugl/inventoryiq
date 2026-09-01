package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.util.List;

/**
 * Parámetros de entrada de IngestCsvFileUseCase (Sección 9.9).
 *
 * El parseo del CSV subido (estructura de columnas, tipos, reglas de
 * negocio ya cubiertas por el propio constructor de Sale) ocurre en el
 * adaptador (adapters/in/csv/SalesCsvRowParser) antes de llegar acá: es
 * un problema de formato/E-S, no de aplicación. Esta capa solo recibe
 * candidateRows (ya parseadas a Sale) y preValidationRejections (lo que
 * el parser ya descartó), y agrega lo que sí necesita puertos de
 * aplicación: integridad referencial y duplicados.
 */
public record IngestCsvFileCommand(
		CsvFileType fileType,
		int totalRowsRead,
		List<CandidateRow> candidateRows,
		List<RowRejection> preValidationRejections) {

	public IngestCsvFileCommand {
		if (fileType == null) {
			throw new InvalidDomainDataException("fileType is required");
		}
		if (candidateRows == null) {
			candidateRows = List.of();
		}
		if (preValidationRejections == null) {
			preValidationRejections = List.of();
		}
	}
}

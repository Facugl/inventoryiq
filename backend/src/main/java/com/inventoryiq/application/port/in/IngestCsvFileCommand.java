package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.util.List;

/**
 * Parámetros de entrada de IngestCsvFileUseCase (Sección 9.9).
 *
 * El parseo del CSV subido (estructura de columnas, tipos, reglas de
 * negocio ya cubiertas por el propio constructor de Sale/Inventory)
 * ocurre en el adaptador (adapters/in/csv/SalesCsvRowParser,
 * adapters/in/csv/InventoryCsvRowParser) antes de llegar acá: es un
 * problema de formato/E-S, no de aplicación. Esta capa solo recibe las
 * filas ya parseadas y preValidationRejections (lo que el parser ya
 * descartó), y agrega lo que sí necesita puertos de aplicación:
 * integridad referencial y duplicados.
 *
 * Las filas vienen en dos listas tipadas —candidateRows para SALES,
 * inventoryCandidateRows para INVENTORY— en vez de una sola lista
 * genérica: solo la que corresponde al fileType se consume, y así el
 * camino de ventas no cambia de forma. El constructor de 4 argumentos
 * es la forma abreviada del caso SALES.
 */
public record IngestCsvFileCommand(
		CsvFileType fileType,
		int totalRowsRead,
		List<CandidateRow> candidateRows,
		List<InventoryCandidateRow> inventoryCandidateRows,
		List<RowRejection> preValidationRejections) {

	public IngestCsvFileCommand {
		if (fileType == null) {
			throw new InvalidDomainDataException("fileType is required");
		}
		if (candidateRows == null) {
			candidateRows = List.of();
		}
		if (inventoryCandidateRows == null) {
			inventoryCandidateRows = List.of();
		}
		if (preValidationRejections == null) {
			preValidationRejections = List.of();
		}
	}

	/** Forma abreviada para ventas: no hay filas de inventario en el lote. */
	public IngestCsvFileCommand(
			CsvFileType fileType,
			int totalRowsRead,
			List<CandidateRow> candidateRows,
			List<RowRejection> preValidationRejections) {
		this(fileType, totalRowsRead, candidateRows, List.of(), preValidationRejections);
	}
}

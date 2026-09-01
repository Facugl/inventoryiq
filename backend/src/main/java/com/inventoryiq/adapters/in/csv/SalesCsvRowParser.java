package com.inventoryiq.adapters.in.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.in.CandidateRow;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.Sale;
import org.apache.commons.csv.CSVRecord;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parsea un CSV de ventas subido (Sección 9.9) fila por fila a candidatos
 * Sale o a rechazos con motivo. Vive en adapters/, no en application/,
 * porque parsear texto CSV a tipos (y las reglas de negocio que ya
 * verifica el propio constructor de Sale, como unidades_vendidas >= 0)
 * es un problema de formato/E-S, no de orquestación de casos de uso —
 * reutiliza CsvFieldParsers, el mismo helper que usan los adaptadores de
 * lectura de adapters/out/csv.
 *
 * Solo valida estructura y tipos por fila; la integridad referencial
 * (producto_id/sucursal_id existen) y los duplicados quedan para la capa
 * de aplicación, que sí tiene los puertos para resolverlos.
 */
public final class SalesCsvRowParser {

	private static final Set<String> EXPECTED_COLUMNS =
			Set.of("venta_id", "fecha", "producto_id", "sucursal_id", "unidades_vendidas", "importe_total");

	private SalesCsvRowParser() {
	}

	public record ParsedBatch(int totalRowsRead, List<CandidateRow> candidateRows, List<RowRejection> preValidationRejections) {
	}

	public static ParsedBatch parse(Reader reader) {
		CsvFileReader.CsvContent content = CsvFileReader.readContent(reader);

		if (!content.headerColumns().containsAll(EXPECTED_COLUMNS)) {
			throw new InvalidDomainDataException(
					"El archivo no tiene las columnas esperadas para ventas: " + EXPECTED_COLUMNS);
		}

		List<CandidateRow> candidates = new ArrayList<>();
		List<RowRejection> rejections = new ArrayList<>();

		int rowNumber = 0;
		for (CSVRecord record : content.records()) {
			rowNumber++;
			try {
				candidates.add(new CandidateRow(rowNumber, toSale(record)));
			} catch (RuntimeException e) {
				rejections.add(new RowRejection(rowNumber, e.getMessage()));
			}
		}

		return new ParsedBatch(content.records().size(), candidates, rejections);
	}

	private static Sale toSale(CSVRecord record) {
		return new Sale(
				CsvFieldParsers.parseLong(record.get("venta_id")),
				CsvFieldParsers.parseDate(record.get("fecha")),
				CsvFieldParsers.parseLong(record.get("producto_id")),
				CsvFieldParsers.parseLong(record.get("sucursal_id")),
				CsvFieldParsers.parseInt(record.get("unidades_vendidas")),
				CsvFieldParsers.parseDecimal(record.get("importe_total")));
	}
}

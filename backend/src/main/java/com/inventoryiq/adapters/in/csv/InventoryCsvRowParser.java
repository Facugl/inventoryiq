package com.inventoryiq.adapters.in.csv;

import com.inventoryiq.adapters.out.csv.parser.CsvFieldParsers;
import com.inventoryiq.adapters.out.csv.parser.CsvFileReader;
import com.inventoryiq.application.port.in.InventoryCandidateRow;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.Inventory;
import org.apache.commons.csv.CSVRecord;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parsea un CSV de inventario subido fila por fila a candidatos
 * Inventory o a rechazos con motivo. Análogo a SalesCsvRowParser, y por
 * el mismo motivo vive en adapters/ y no en application/: parsear texto
 * CSV a tipos (y las reglas de negocio que ya verifica el propio
 * constructor de Inventory, como stock_actual >= 0) es un problema de
 * formato/E-S, no de orquestación de casos de uso.
 *
 * Las columnas esperadas son las de inventario.csv (Sección 5.7) menos
 * inventario_id: a diferencia de venta_id en ventas, el id del snapshot
 * no lo elige quien sube el archivo — lo asigna el adaptador de
 * persistencia (ver InventoryIngestionRepository). Un archivo que igual
 * traiga la columna inventario_id se acepta y esa columna se ignora,
 * porque el chequeo de header es containsAll, no igualdad exacta.
 *
 * Solo valida estructura y tipos por fila; la integridad referencial
 * (producto_id/sucursal_id existen) y los duplicados quedan para la capa
 * de aplicación, que sí tiene los puertos para resolverlos.
 */
public final class InventoryCsvRowParser {

	private static final Set<String> EXPECTED_COLUMNS =
			Set.of("fecha_snapshot", "producto_id", "sucursal_id", "stock_actual", "stock_en_transito");

	private InventoryCsvRowParser() {
	}

	public record ParsedBatch(int totalRowsRead, List<InventoryCandidateRow> candidateRows, List<RowRejection> preValidationRejections) {
	}

	public static ParsedBatch parse(Reader reader) {
		CsvFileReader.CsvContent content = CsvFileReader.readContent(reader);

		if (!content.headerColumns().containsAll(EXPECTED_COLUMNS)) {
			throw new InvalidDomainDataException(
					"El archivo no tiene las columnas esperadas para inventario: " + EXPECTED_COLUMNS);
		}

		List<InventoryCandidateRow> candidates = new ArrayList<>();
		List<RowRejection> rejections = new ArrayList<>();

		int rowNumber = 0;
		for (CSVRecord record : content.records()) {
			rowNumber++;
			try {
				candidates.add(new InventoryCandidateRow(rowNumber, toInventory(record)));
			} catch (RuntimeException e) {
				rejections.add(new RowRejection(rowNumber, e.getMessage()));
			}
		}

		return new ParsedBatch(content.records().size(), candidates, rejections);
	}

	private static Inventory toInventory(CSVRecord record) {
		return new Inventory(
				null,
				CsvFieldParsers.parseDate(record.get("fecha_snapshot")),
				CsvFieldParsers.parseLong(record.get("producto_id")),
				CsvFieldParsers.parseLong(record.get("sucursal_id")),
				CsvFieldParsers.parseInt(record.get("stock_actual")),
				CsvFieldParsers.parseInt(record.get("stock_en_transito")));
	}
}

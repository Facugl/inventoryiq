package com.inventoryiq.adapters.in.csv;

import com.inventoryiq.application.port.in.InventoryCandidateRow;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryCsvRowParserTest {

	private static final String HEADER = "fecha_snapshot,producto_id,sucursal_id,stock_actual,stock_en_transito\n";

	@Test
	void parsesValidRowsIntoCandidatesWith1BasedRowNumbers() {
		String csv = HEADER
				+ "2025-07-03,1001,1,314,0\n"
				+ "2025-07-04,1001,1,259,120\n";

		InventoryCsvRowParser.ParsedBatch batch = InventoryCsvRowParser.parse(new StringReader(csv));

		assertEquals(2, batch.totalRowsRead());
		assertEquals(2, batch.candidateRows().size());
		assertEquals(0, batch.preValidationRejections().size());

		InventoryCandidateRow first = batch.candidateRows().get(0);
		assertEquals(1, first.rowNumber());
		assertEquals(LocalDate.parse("2025-07-03"), first.inventory().snapshotDate());
		assertEquals(1001L, first.inventory().productId());
		assertEquals(1L, first.inventory().storeId());
		assertEquals(314, first.inventory().currentStock());
		assertEquals(0, first.inventory().stockInTransit());

		assertEquals(2, batch.candidateRows().get(1).rowNumber());
		assertEquals(120, batch.candidateRows().get(1).inventory().stockInTransit());
	}

	@Test
	void leavesInventoryIdUnassignedBecauseThePersistenceAdapterGeneratesIt() {
		InventoryCsvRowParser.ParsedBatch batch =
				InventoryCsvRowParser.parse(new StringReader(HEADER + "2025-07-03,1001,1,314,0\n"));

		assertNull(batch.candidateRows().get(0).inventory().inventoryId());
	}

	@Test
	void rejectsARowWithNegativeStockButKeepsProcessingTheRest() {
		String csv = HEADER
				+ "2025-07-03,1001,1,-5,0\n"
				+ "2025-07-04,1001,1,259,0\n";

		InventoryCsvRowParser.ParsedBatch batch = InventoryCsvRowParser.parse(new StringReader(csv));

		assertEquals(2, batch.totalRowsRead());
		assertEquals(1, batch.candidateRows().size());
		assertEquals(1, batch.preValidationRejections().size());

		RowRejection rejection = batch.preValidationRejections().get(0);
		assertEquals(1, rejection.rowNumber());
	}

	@Test
	void rejectsARowWithNegativeStockInTransit() {
		InventoryCsvRowParser.ParsedBatch batch =
				InventoryCsvRowParser.parse(new StringReader(HEADER + "2025-07-03,1001,1,314,-2\n"));

		assertTrue(batch.candidateRows().isEmpty());
		assertEquals(1, batch.preValidationRejections().size());
	}

	@Test
	void rejectsARowWithAMalformedDate() {
		InventoryCsvRowParser.ParsedBatch batch =
				InventoryCsvRowParser.parse(new StringReader(HEADER + "not-a-date,1001,1,314,0\n"));

		assertTrue(batch.candidateRows().isEmpty());
		assertEquals(1, batch.preValidationRejections().size());
	}

	@Test
	void rejectsARowWithANonNumericStock() {
		InventoryCsvRowParser.ParsedBatch batch =
				InventoryCsvRowParser.parse(new StringReader(HEADER + "2025-07-03,1001,1,abc,0\n"));

		assertTrue(batch.candidateRows().isEmpty());
		assertEquals(1, batch.preValidationRejections().size());
	}

	@Test
	void throwsWhenAnExpectedColumnIsMissing() {
		String csvWithoutStockEnTransito = "fecha_snapshot,producto_id,sucursal_id,stock_actual\n"
				+ "2025-07-03,1001,1,314\n";

		assertThrows(InvalidDomainDataException.class,
				() -> InventoryCsvRowParser.parse(new StringReader(csvWithoutStockEnTransito)));
	}

	/** El chequeo de header es containsAll: un export con la columna inventario_id se acepta y esa columna se ignora. */
	@Test
	void acceptsAFileThatAlsoCarriesTheInventarioIdColumnAndIgnoresIt() {
		String csv = "inventario_id,fecha_snapshot,producto_id,sucursal_id,stock_actual,stock_en_transito\n"
				+ "900000,2025-07-03,1001,1,314,0\n";

		InventoryCsvRowParser.ParsedBatch batch = InventoryCsvRowParser.parse(new StringReader(csv));

		assertEquals(1, batch.candidateRows().size());
		assertNull(batch.candidateRows().get(0).inventory().inventoryId());
	}
}

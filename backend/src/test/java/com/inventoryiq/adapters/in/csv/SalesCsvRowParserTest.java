package com.inventoryiq.adapters.in.csv;

import com.inventoryiq.application.port.in.CandidateRow;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesCsvRowParserTest {

	private static final String HEADER = "venta_id,fecha,producto_id,sucursal_id,unidades_vendidas,importe_total\n";

	@Test
	void parsesValidRowsIntoCandidatesWith1BasedRowNumbers() {
		String csv = HEADER
				+ "500001,2025-07-03,1001,1,43,26854.79\n"
				+ "500002,2025-07-04,1001,1,55,34349.15\n";

		SalesCsvRowParser.ParsedBatch batch = SalesCsvRowParser.parse(new StringReader(csv));

		assertEquals(2, batch.totalRowsRead());
		assertEquals(2, batch.candidateRows().size());
		assertEquals(0, batch.preValidationRejections().size());

		CandidateRow first = batch.candidateRows().get(0);
		assertEquals(1, first.rowNumber());
		assertEquals(1001L, first.sale().productId());
		assertEquals(43, first.sale().unitsSold());

		assertEquals(2, batch.candidateRows().get(1).rowNumber());
	}

	@Test
	void rejectsARowWithNegativeUnitsSoldButKeepsProcessingTheRest() {
		String csv = HEADER
				+ "500001,2025-07-03,1001,1,-5,26854.79\n"
				+ "500002,2025-07-04,1001,1,55,34349.15\n";

		SalesCsvRowParser.ParsedBatch batch = SalesCsvRowParser.parse(new StringReader(csv));

		assertEquals(2, batch.totalRowsRead());
		assertEquals(1, batch.candidateRows().size());
		assertEquals(1, batch.preValidationRejections().size());

		RowRejection rejection = batch.preValidationRejections().get(0);
		assertEquals(1, rejection.rowNumber());
	}

	@Test
	void rejectsARowWithAMalformedDate() {
		String csv = HEADER + "500001,not-a-date,1001,1,43,26854.79\n";

		SalesCsvRowParser.ParsedBatch batch = SalesCsvRowParser.parse(new StringReader(csv));

		assertTrue(batch.candidateRows().isEmpty());
		assertEquals(1, batch.preValidationRejections().size());
	}

	@Test
	void rejectsARowWithANonNumericQuantity() {
		String csv = HEADER + "500001,2025-07-03,1001,1,abc,26854.79\n";

		SalesCsvRowParser.ParsedBatch batch = SalesCsvRowParser.parse(new StringReader(csv));

		assertTrue(batch.candidateRows().isEmpty());
		assertEquals(1, batch.preValidationRejections().size());
	}

	@Test
	void throwsWhenAnExpectedColumnIsMissing() {
		String csvWithoutImporteTotal = "venta_id,fecha,producto_id,sucursal_id,unidades_vendidas\n"
				+ "500001,2025-07-03,1001,1,43\n";

		assertThrows(InvalidDomainDataException.class,
				() -> SalesCsvRowParser.parse(new StringReader(csvWithoutImporteTotal)));
	}
}

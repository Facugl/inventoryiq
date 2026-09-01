package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.IngestionSummaryResponse;
import com.inventoryiq.application.port.in.IngestCsvFileResult;
import com.inventoryiq.application.port.in.RowRejection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngestionSummaryResponseMapperTest {

	@Test
	void mapsEveryFieldIncludingRejections() {
		IngestCsvFileResult result = new IngestCsvFileResult(10, 8, 2,
				List.of(new RowRejection(3, "producto_id 9999 no existe en el catálogo"),
						new RowRejection(7, "fecha inválida")));

		IngestionSummaryResponse response = IngestionSummaryResponseMapper.toResponse(result);

		assertEquals(10, response.totalRowsRead());
		assertEquals(8, response.acceptedCount());
		assertEquals(2, response.rejectedCount());
		assertEquals(2, response.rejections().size());
		assertEquals(3, response.rejections().get(0).rowNumber());
		assertEquals("producto_id 9999 no existe en el catálogo", response.rejections().get(0).reason());
	}
}

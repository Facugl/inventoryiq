package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.IngestCsvFileCommand;
import com.inventoryiq.application.port.in.IngestCsvFileResult;
import com.inventoryiq.application.port.in.IngestCsvFileUseCase;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.domain.exception.CsvIngestionThresholdExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el controller de forma aislada: IngestCsvFileUseCase está
 * mockeado, no hay CSV real de por medio.
 */
@WebMvcTest(CsvIngestionController.class)
class CsvIngestionControllerTest {

	private static final String CSV_CONTENT =
			"venta_id,fecha,producto_id,sucursal_id,unidades_vendidas,importe_total\n500001,2025-07-03,1001,1,43,26854.79\n";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IngestCsvFileUseCase ingestCsvFileUseCase;

	@Test
	void returnsTheIngestionSummaryAsJsonForAValidUpload() throws Exception {
		given(ingestCsvFileUseCase.execute(any(IngestCsvFileCommand.class)))
				.willReturn(new IngestCsvFileResult(1, 1, 0, List.of()));

		MockMultipartFile file = new MockMultipartFile("file", "ventas.csv", "text/csv", CSV_CONTENT.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file).param("fileType", "SALES"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalRowsRead").value(1))
				.andExpect(jsonPath("$.acceptedCount").value(1))
				.andExpect(jsonPath("$.rejectedCount").value(0));
	}

	@Test
	void returnsTheRejectionListWhenSomeRowsFail() throws Exception {
		given(ingestCsvFileUseCase.execute(any(IngestCsvFileCommand.class)))
				.willReturn(new IngestCsvFileResult(2, 1, 1, List.of(new RowRejection(2, "producto_id 9999 no existe"))));

		MockMultipartFile file = new MockMultipartFile("file", "ventas.csv", "text/csv", CSV_CONTENT.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file).param("fileType", "SALES"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rejections[0].rowNumber").value(2))
				.andExpect(jsonPath("$.rejections[0].reason").value("producto_id 9999 no existe"));
	}

	@Test
	void returns422WhenTheRejectionThresholdIsExceeded() throws Exception {
		given(ingestCsvFileUseCase.execute(any(IngestCsvFileCommand.class)))
				.willThrow(new CsvIngestionThresholdExceededException(20, 5, 25.0, List.of()));

		MockMultipartFile file = new MockMultipartFile("file", "ventas.csv", "text/csv", CSV_CONTENT.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file).param("fileType", "SALES"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.rejectedCount").value(5));
	}

	@Test
	void returns400WhenFileTypeIsUnsupported() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "compras.csv", "text/csv", CSV_CONTENT.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file).param("fileType", "PURCHASES"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenFileTypeIsMissing() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "ventas.csv", "text/csv", CSV_CONTENT.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file))
				.andExpect(status().isBadRequest());
	}
}

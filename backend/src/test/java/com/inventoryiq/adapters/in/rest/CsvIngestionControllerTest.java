package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.CsvFileType;
import com.inventoryiq.application.port.in.IngestCsvFileCommand;
import com.inventoryiq.application.port.in.IngestCsvFileResult;
import com.inventoryiq.application.port.in.IngestCsvFileUseCase;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.domain.exception.CsvIngestionThresholdExceededException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
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

	/** El fileType elige el parser del adaptador: con INVENTORY el command tiene que llegar con filas de inventario. */
	@Test
	void parsesAnInventoryUploadIntoInventoryCandidateRows() throws Exception {
		given(ingestCsvFileUseCase.execute(any(IngestCsvFileCommand.class)))
				.willReturn(new IngestCsvFileResult(1, 1, 0, List.of()));

		String inventoryCsv = "fecha_snapshot,producto_id,sucursal_id,stock_actual,stock_en_transito\n2025-07-03,1001,1,314,0\n";
		MockMultipartFile file = new MockMultipartFile(
				"file", "inventario.csv", "text/csv", inventoryCsv.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file).param("fileType", "INVENTORY"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.acceptedCount").value(1));

		ArgumentCaptor<IngestCsvFileCommand> captor = ArgumentCaptor.forClass(IngestCsvFileCommand.class);
		verify(ingestCsvFileUseCase).execute(captor.capture());
		IngestCsvFileCommand command = captor.getValue();

		assertEquals(CsvFileType.INVENTORY, command.fileType());
		assertEquals(1, command.inventoryCandidateRows().size());
		assertTrue(command.candidateRows().isEmpty());
		assertEquals(1001L, command.inventoryCandidateRows().get(0).inventory().productId());
		assertEquals(314, command.inventoryCandidateRows().get(0).inventory().currentStock());
	}

	/** Un CSV de ventas enviado como INVENTORY no tiene las columnas esperadas: 400 por InvalidDomainDataException. */
	@Test
	void returns400WhenTheUploadedColumnsDoNotMatchTheDeclaredFileType() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "ventas.csv", "text/csv", CSV_CONTENT.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file).param("fileType", "INVENTORY"))
				.andExpect(status().isBadRequest());
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

package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.csv.SalesCsvRowParser;
import com.inventoryiq.adapters.in.rest.dto.IngestionSummaryResponse;
import com.inventoryiq.adapters.in.rest.mapper.IngestionSummaryResponseMapper;
import com.inventoryiq.application.port.in.CsvFileType;
import com.inventoryiq.application.port.in.IngestCsvFileCommand;
import com.inventoryiq.application.port.in.IngestCsvFileUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Adaptador de entrada REST para IngestCsvFileUseCase (Sección 8.13).
 * Depende únicamente del puerto de entrada, nunca de IngestCsvFileService
 * — el wiring concreto vive en config/UseCaseConfig.
 *
 * fileType se recibe directamente como CsvFileType: Spring convierte el
 * valor del form field al nombre de la constante del enum, y si no
 * matchea (cualquier valor que no sea SALES, el único implementado en
 * este slice) dispara MethodArgumentTypeMismatchException — 400, sin
 * agregar un handler de error nuevo, mismo mecanismo que sortBy en
 * OverstockController.
 */
@RestController
@RequestMapping("/api/v1/csv-ingestions")
@Validated
public class CsvIngestionController {

	private final IngestCsvFileUseCase ingestCsvFileUseCase;

	public CsvIngestionController(IngestCsvFileUseCase ingestCsvFileUseCase) {
		this.ingestCsvFileUseCase = ingestCsvFileUseCase;
	}

	@PostMapping
	public IngestionSummaryResponse ingest(
			@RequestParam CsvFileType fileType,
			@RequestParam("file") MultipartFile file) throws IOException {

		SalesCsvRowParser.ParsedBatch batch;
		try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
			batch = SalesCsvRowParser.parse(reader);
		}

		IngestCsvFileCommand command = new IngestCsvFileCommand(
				fileType, batch.totalRowsRead(), batch.candidateRows(), batch.preValidationRejections());

		return IngestionSummaryResponseMapper.toResponse(ingestCsvFileUseCase.execute(command));
	}
}

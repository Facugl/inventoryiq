package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.csv.InventoryCsvRowParser;
import com.inventoryiq.adapters.in.csv.SalesCsvRowParser;
import com.inventoryiq.adapters.in.rest.dto.IngestionSummaryResponse;
import com.inventoryiq.adapters.in.rest.mapper.IngestionSummaryResponseMapper;
import com.inventoryiq.application.port.in.CsvFileType;
import com.inventoryiq.application.port.in.IngestCsvFileCommand;
import com.inventoryiq.application.port.in.IngestCsvFileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.List;

/**
 * Adaptador de entrada REST para IngestCsvFileUseCase (Sección 8.13).
 * Depende únicamente del puerto de entrada, nunca de IngestCsvFileService
 * — el wiring concreto vive en config/UseCaseConfig.
 *
 * fileType se recibe directamente como CsvFileType: Spring convierte el
 * valor del form field al nombre de la constante del enum, y si no
 * matchea (cualquier valor que no sea SALES o INVENTORY, los dos
 * implementados) dispara MethodArgumentTypeMismatchException — 400, sin
 * agregar un handler de error nuevo, mismo mecanismo que sortBy en
 * OverstockController.
 *
 * El enum sí decide qué parser del adaptador se usa: cada tipo de
 * archivo tiene columnas propias, así que la conversión CSV -> filas
 * candidatas no puede ser única. Lo que viene después (validación
 * referencial, duplicados, umbral, persistencia) ya es responsabilidad
 * del use case, igual para los dos tipos.
 */
@RestController
@RequestMapping("/api/v1/csv-ingestions")
@Validated
@Tag(name = "Ingesta CSV", description = "Carga de archivos CSV para su procesamiento")
public class CsvIngestionController {

	private final IngestCsvFileUseCase ingestCsvFileUseCase;

	public CsvIngestionController(IngestCsvFileUseCase ingestCsvFileUseCase) {
		this.ingestCsvFileUseCase = ingestCsvFileUseCase;
	}

	@PostMapping
	@Operation(summary = "Ingerir un archivo CSV",
			description = "fileType implementados: SALES e INVENTORY. Todo-o-nada por lote: si el porcentaje de filas "
					+ "rechazadas supera el umbral configurado, no se persiste ninguna fila (422).")
	public IngestionSummaryResponse ingest(
			@RequestParam CsvFileType fileType,
			@RequestParam("file") MultipartFile file) throws IOException {

		IngestCsvFileCommand command;
		try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
			command = toCommand(fileType, reader);
		}

		return IngestionSummaryResponseMapper.INSTANCE.toResponse(ingestCsvFileUseCase.execute(command));
	}

	private static IngestCsvFileCommand toCommand(CsvFileType fileType, Reader reader) {
		return switch (fileType) {
			case SALES -> {
				SalesCsvRowParser.ParsedBatch batch = SalesCsvRowParser.parse(reader);
				yield new IngestCsvFileCommand(
						fileType, batch.totalRowsRead(), batch.candidateRows(), batch.preValidationRejections());
			}
			case INVENTORY -> {
				InventoryCsvRowParser.ParsedBatch batch = InventoryCsvRowParser.parse(reader);
				yield new IngestCsvFileCommand(
						fileType, batch.totalRowsRead(), List.of(), batch.candidateRows(), batch.preValidationRejections());
			}
		};
	}
}

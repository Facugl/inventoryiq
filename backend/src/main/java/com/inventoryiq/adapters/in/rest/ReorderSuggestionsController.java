package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.ReorderSuggestionResponse;
import com.inventoryiq.adapters.in.rest.mapper.ReorderSuggestionResponseMapper;
import com.inventoryiq.application.port.in.GenerateReorderSuggestionsQuery;
import com.inventoryiq.application.port.in.GenerateReorderSuggestionsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Adaptador de entrada REST para GenerateReorderSuggestionsUseCase
 * (Sección 8.5). Depende únicamente del puerto de entrada
 * (GenerateReorderSuggestionsUseCase), nunca de GenerateReorderSuggestionsService
 * — el wiring concreto vive en config/UseCaseConfig.
 *
 * Sin el filtro "estado" que lista la Sección 8.5 (pendiente/aplicada/
 * descartada): implica persistencia, ver el Javadoc de
 * GenerateReorderSuggestionsQuery.
 */
@RestController
@RequestMapping("/api/v1/reorder-suggestions")
@Validated
@Tag(name = "Sugerencias de reposición", description = "Cálculo en memoria, sin persistir (ver Recomendaciones para la versión persistida)")
public class ReorderSuggestionsController {

	private final GenerateReorderSuggestionsUseCase generateReorderSuggestionsUseCase;

	public ReorderSuggestionsController(GenerateReorderSuggestionsUseCase generateReorderSuggestionsUseCase) {
		this.generateReorderSuggestionsUseCase = generateReorderSuggestionsUseCase;
	}

	@GetMapping
	@Operation(summary = "Generar sugerencias de reposición",
			description = "Para cada producto del alcance que dispara el punto de pedido: cantidad sugerida, fecha "
					+ "límite de emisión y justificación. No persiste nada (a diferencia de POST /recommendations/recalculate).")
	public List<ReorderSuggestionResponse> getReorderSuggestions(
			@RequestParam @NotNull @Positive Long storeId,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(required = false) @Positive Long supplierId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {

		GenerateReorderSuggestionsQuery query = new GenerateReorderSuggestionsQuery(storeId, categoryId, supplierId, referenceDate);

		return generateReorderSuggestionsUseCase.execute(query).stream()
				.map(ReorderSuggestionResponseMapper::toResponse)
				.toList();
	}
}

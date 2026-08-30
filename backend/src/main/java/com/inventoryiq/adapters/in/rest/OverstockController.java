package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.OverstockProductResponse;
import com.inventoryiq.adapters.in.rest.mapper.OverstockProductResponseMapper;
import com.inventoryiq.application.port.in.DetectOverstockQuery;
import com.inventoryiq.application.port.in.DetectOverstockUseCase;
import com.inventoryiq.application.port.in.OverstockSortBy;
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
 * Adaptador de entrada REST para DetectOverstockUseCase (Sección 8.4).
 * Depende únicamente del puerto de entrada (DetectOverstockUseCase), nunca
 * de DetectOverstockService ni de los adaptadores CSV — el wiring concreto
 * vive en config/UseCaseConfig.
 *
 * sortBy se recibe directamente como OverstockSortBy: Spring convierte el
 * valor de query string al nombre de la constante del enum
 * (IMMOBILIZED_VALUE / DAYS_OF_COVERAGE), y si no matchea, dispara
 * MethodArgumentTypeMismatchException — el mismo mecanismo que ya usamos
 * para validar storeId/referenceDate mal formados, sin agregar un handler
 * de error nuevo.
 */
@RestController
@RequestMapping("/api/v1/products")
@Validated
public class OverstockController {

	private final DetectOverstockUseCase detectOverstockUseCase;

	public OverstockController(DetectOverstockUseCase detectOverstockUseCase) {
		this.detectOverstockUseCase = detectOverstockUseCase;
	}

	@GetMapping("/overstock")
	public List<OverstockProductResponse> getOverstockProducts(
			@RequestParam @NotNull @Positive Long storeId,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
			@RequestParam(required = false, defaultValue = "IMMOBILIZED_VALUE") OverstockSortBy sortBy) {

		DetectOverstockQuery query = DetectOverstockQuery.of(storeId, categoryId, referenceDate, sortBy);

		return detectOverstockUseCase.execute(query).stream()
				.map(OverstockProductResponseMapper::toResponse)
				.toList();
	}
}

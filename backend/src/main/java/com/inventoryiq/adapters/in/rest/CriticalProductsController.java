package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.CriticalProductResponse;
import com.inventoryiq.adapters.in.rest.mapper.CriticalProductResponseMapper;
import com.inventoryiq.application.port.in.GetCriticalProductsQuery;
import com.inventoryiq.application.port.in.GetCriticalProductsUseCase;
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
 * Adaptador de entrada REST para GetCriticalProductsUseCase (Sección 8.3).
 * Depende únicamente del puerto de entrada (GetCriticalProductsUseCase),
 * nunca de GetCriticalProductsService ni de los adaptadores CSV — el
 * wiring concreto vive en config/UseCaseConfig.
 *
 * referenceDate es obligatorio a propósito: GetCriticalProductsQuery nunca
 * resuelve "hoy" internamente (los CSV son una simulación histórica con
 * fecha de corte fija), así que es este adaptador el que debe recibirlo
 * explícitamente de quien llama, no asumir LocalDate.now().
 */
@RestController
@RequestMapping("/api/v1/products")
@Validated
public class CriticalProductsController {

	private final GetCriticalProductsUseCase getCriticalProductsUseCase;

	public CriticalProductsController(GetCriticalProductsUseCase getCriticalProductsUseCase) {
		this.getCriticalProductsUseCase = getCriticalProductsUseCase;
	}

	@GetMapping("/critical")
	public List<CriticalProductResponse> getCriticalProducts(
			@RequestParam @NotNull @Positive Long storeId,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(required = false) @Positive Integer limit,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {

		GetCriticalProductsQuery query = GetCriticalProductsQuery.of(storeId, categoryId, limit, referenceDate);

		return getCriticalProductsUseCase.execute(query).stream()
				.map(CriticalProductResponseMapper::toResponse)
				.toList();
	}
}

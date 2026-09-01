package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.ForecastDemandResponse;
import com.inventoryiq.adapters.in.rest.mapper.ForecastDemandResponseMapper;
import com.inventoryiq.application.port.in.ForecastDemandQuery;
import com.inventoryiq.application.port.in.ForecastDemandUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Adaptador de entrada REST para ForecastDemandUseCase (Sección 9.4, sin
 * endpoint documentado en la Sección 8 — diseñado en este slice). Depende
 * únicamente del puerto de entrada, nunca de ForecastDemandService — el
 * wiring concreto vive en config/UseCaseConfig.
 *
 * productId va en el path, a diferencia de storeId (query param en todo
 * el proyecto): acá sí identifica un único recurso puntual a buscar
 * (lookup por id, con 404 si no existe), no un filtro de alcance sobre un
 * catálogo.
 */
@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ForecastController {

	private final ForecastDemandUseCase forecastDemandUseCase;

	public ForecastController(ForecastDemandUseCase forecastDemandUseCase) {
		this.forecastDemandUseCase = forecastDemandUseCase;
	}

	@GetMapping("/{productId}/forecast")
	public ForecastDemandResponse getForecast(
			@PathVariable @Positive Long productId,
			@RequestParam @NotNull @Positive Long storeId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
			@RequestParam @Positive int horizonDays) {

		ForecastDemandQuery query = new ForecastDemandQuery(productId, storeId, referenceDate, horizonDays);

		return ForecastDemandResponseMapper.toResponse(forecastDemandUseCase.execute(query));
	}
}

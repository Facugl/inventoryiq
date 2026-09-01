package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.RecalculateProductStatusRequest;
import com.inventoryiq.adapters.in.rest.dto.RecalculateProductStatusResponse;
import com.inventoryiq.adapters.in.rest.mapper.RecalculateProductStatusResponseMapper;
import com.inventoryiq.application.port.in.RecalculateProductStatusCommand;
import com.inventoryiq.application.port.in.RecalculateProductStatusUseCase;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Adaptador de entrada REST para RecalculateProductStatusUseCase
 * (Sección 9.10, sin endpoint documentado en la Sección 8 — diseñado en
 * este slice para poder disparar el job manualmente, además del trigger
 * programado real en adapters/in/scheduled). Depende únicamente del
 * puerto de entrada, nunca de RecalculateProductStatusService — el
 * wiring concreto vive en config/UseCaseConfig.
 *
 * storeId es opcional en el body (a diferencia de todo el resto del
 * proyecto): si se omite, procesa todas las sucursales activas, tal
 * como describe la Sección 9.10. referenceDate no es un parámetro de
 * request (ningún trigger de este caso de uso lo expone): se resuelve
 * acá con el reloj inyectado, igual que RecommendationsController.
 */
@RestController
@RequestMapping("/api/v1/product-status")
@Validated
public class ProductStatusController {

	private final RecalculateProductStatusUseCase recalculateProductStatusUseCase;
	private final Clock clock;

	public ProductStatusController(RecalculateProductStatusUseCase recalculateProductStatusUseCase, Clock clock) {
		this.recalculateProductStatusUseCase = recalculateProductStatusUseCase;
		this.clock = clock;
	}

	@PostMapping("/recalculate")
	public RecalculateProductStatusResponse recalculate(@RequestBody(required = false) @Valid RecalculateProductStatusRequest request) {
		Long storeId = request != null ? request.storeId() : null;
		LocalDate referenceDate = LocalDate.now(clock);

		RecalculateProductStatusCommand command = new RecalculateProductStatusCommand(storeId, referenceDate);

		return RecalculateProductStatusResponseMapper.toResponse(recalculateProductStatusUseCase.execute(command));
	}
}

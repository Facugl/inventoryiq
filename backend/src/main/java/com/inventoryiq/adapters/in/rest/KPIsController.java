package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.InventoryKPIsResponse;
import com.inventoryiq.adapters.in.rest.mapper.InventoryKPIsResponseMapper;
import com.inventoryiq.application.port.in.CalculateInventoryKPIsQuery;
import com.inventoryiq.application.port.in.CalculateInventoryKPIsUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Adaptador de entrada REST para CalculateInventoryKPIsUseCase (Sección
 * 8.8). Depende únicamente del puerto de entrada, nunca de
 * CalculateInventoryKPIsService — el wiring concreto vive en
 * config/UseCaseConfig.
 */
@RestController
@RequestMapping("/api/v1/kpis")
@Validated
public class KPIsController {

	private final CalculateInventoryKPIsUseCase calculateInventoryKPIsUseCase;

	public KPIsController(CalculateInventoryKPIsUseCase calculateInventoryKPIsUseCase) {
		this.calculateInventoryKPIsUseCase = calculateInventoryKPIsUseCase;
	}

	@GetMapping
	public InventoryKPIsResponse getKPIs(
			@RequestParam @NotNull @Positive Long storeId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

		CalculateInventoryKPIsQuery query = new CalculateInventoryKPIsQuery(storeId, fromDate, toDate);

		return InventoryKPIsResponseMapper.toResponse(calculateInventoryKPIsUseCase.execute(query));
	}
}

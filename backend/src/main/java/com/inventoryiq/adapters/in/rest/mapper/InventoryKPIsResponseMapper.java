package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.InventoryKPIsResponse;
import com.inventoryiq.application.port.in.InventoryKPIsResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class InventoryKPIsResponseMapper {
	private InventoryKPIsResponseMapper() {
	}

	public static InventoryKPIsResponse toResponse(InventoryKPIsResult result) {
		return new InventoryKPIsResponse(
				result.stockoutRate(),
				result.averageDaysOfCoverage(),
				result.immobilizedOverstockValue(),
				result.recommendationsFollowedRate(),
				result.inventoryTurnover());
	}
}

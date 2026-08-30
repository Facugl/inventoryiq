package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.CriticalProductResponse;
import com.inventoryiq.application.port.in.CriticalProductResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class CriticalProductResponseMapper {
	private CriticalProductResponseMapper() {
	}

	public static CriticalProductResponse toResponse(CriticalProductResult result) {
		return new CriticalProductResponse(
				result.productId(),
				result.sku(),
				result.productName(),
				result.storeId(),
				result.categoryId(),
				result.currentStock(),
				result.reorderPoint().units(),
				result.currentDaysOfCoverage(),
				result.status(),
				result.criticalityLevel().score());
	}
}

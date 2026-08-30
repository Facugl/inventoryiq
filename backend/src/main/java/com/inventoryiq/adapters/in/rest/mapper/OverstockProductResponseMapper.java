package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.OverstockProductResponse;
import com.inventoryiq.application.port.in.OverstockProductResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class OverstockProductResponseMapper {
	private OverstockProductResponseMapper() {
	}

	public static OverstockProductResponse toResponse(OverstockProductResult result) {
		return new OverstockProductResponse(
				result.productId(),
				result.sku(),
				result.productName(),
				result.storeId(),
				result.categoryId(),
				result.currentStock(),
				result.currentDaysOfCoverage(),
				result.immobilizedValue());
	}
}

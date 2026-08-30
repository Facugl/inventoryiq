package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.AlertResponse;
import com.inventoryiq.application.port.in.AlertResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class AlertResponseMapper {
	private AlertResponseMapper() {
	}

	public static AlertResponse toResponse(AlertResult result) {
		return new AlertResponse(
				result.productId(),
				result.sku(),
				result.productName(),
				result.storeId(),
				result.categoryId(),
				result.type(),
				result.severity(),
				result.generatedAt());
	}
}

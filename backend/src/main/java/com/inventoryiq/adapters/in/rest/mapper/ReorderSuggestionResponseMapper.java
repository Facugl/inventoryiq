package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.ReorderSuggestionResponse;
import com.inventoryiq.application.port.in.ReorderSuggestionResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class ReorderSuggestionResponseMapper {
	private ReorderSuggestionResponseMapper() {
	}

	public static ReorderSuggestionResponse toResponse(ReorderSuggestionResult result) {
		return new ReorderSuggestionResponse(
				result.productId(),
				result.sku(),
				result.productName(),
				result.storeId(),
				result.categoryId(),
				result.supplierId(),
				result.suggestedQuantity(),
				result.orderDeadlineDate(),
				result.justification());
	}
}

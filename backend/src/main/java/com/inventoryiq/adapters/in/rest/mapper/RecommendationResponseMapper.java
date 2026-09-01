package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.RecommendationResponse;
import com.inventoryiq.application.port.in.RecommendationResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class RecommendationResponseMapper {
	private RecommendationResponseMapper() {
	}

	public static RecommendationResponse toResponse(RecommendationResult result) {
		return new RecommendationResponse(
				result.recommendationId(),
				result.productId(),
				result.sku(),
				result.productName(),
				result.storeId(),
				result.categoryId(),
				result.supplierId(),
				result.suggestedQuantity(),
				result.orderDeadlineDate(),
				result.justification(),
				result.status(),
				result.generationDate(),
				result.feedbackComment(),
				result.feedbackDate());
	}
}

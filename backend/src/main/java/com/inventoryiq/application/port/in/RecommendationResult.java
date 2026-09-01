package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.model.RecommendationStatus;

import java.time.LocalDate;

/** Forma de aplicación de una recomendación persistida (Secciones 8.5/8.6/8.7). */
public record RecommendationResult(
		Long recommendationId,
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		Long supplierId,
		int suggestedQuantity,
		LocalDate orderDeadlineDate,
		String justification,
		RecommendationStatus status,
		LocalDate generationDate,
		String feedbackComment,
		LocalDate feedbackDate) {
}

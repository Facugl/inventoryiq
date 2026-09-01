package com.inventoryiq.adapters.in.rest.dto;

import java.time.LocalDate;

/** Forma JSON pública de una recomendación de compra (GET /api/v1/reorder-suggestions). */
public record ReorderSuggestionResponse(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		Long supplierId,
		int suggestedQuantity,
		LocalDate orderDeadlineDate,
		String justification) {
}

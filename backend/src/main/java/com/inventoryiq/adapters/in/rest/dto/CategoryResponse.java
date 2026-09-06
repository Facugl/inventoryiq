package com.inventoryiq.adapters.in.rest.dto;

/**
 * Forma JSON pública de una categoría (GET /api/v1/categories, PATCH
 * .../parameters). parentCategoryId es null para categorías raíz.
 */
public record CategoryResponse(
		Long categoryId,
		String name,
		Long parentCategoryId,
		int maxCoverageDaysThreshold,
		int defaultExtraCoverageDays) {
}

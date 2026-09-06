package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.CategoryResponse;
import com.inventoryiq.application.port.in.CategoryResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class CategoryResponseMapper {
	private CategoryResponseMapper() {
	}

	public static CategoryResponse toResponse(CategoryResult result) {
		return new CategoryResponse(
				result.categoryId(), result.name(), result.parentCategoryId(),
				result.maxCoverageDaysThreshold(), result.defaultExtraCoverageDays());
	}
}

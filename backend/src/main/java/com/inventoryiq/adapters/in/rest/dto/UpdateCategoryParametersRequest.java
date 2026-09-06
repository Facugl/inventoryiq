package com.inventoryiq.adapters.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Cuerpo de la request de PATCH /api/v1/categories/{categoryId}/parameters. */
public record UpdateCategoryParametersRequest(
		@NotNull @Positive Integer maxCoverageDaysThreshold,
		@NotNull @PositiveOrZero Integer defaultExtraCoverageDays) {
}

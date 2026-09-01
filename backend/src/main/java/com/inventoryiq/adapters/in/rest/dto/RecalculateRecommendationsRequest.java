package com.inventoryiq.adapters.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuerpo de la request de POST /api/v1/recommendations/recalculate (Sección 8.6). */
public record RecalculateRecommendationsRequest(@NotNull @Positive Long storeId) {
}

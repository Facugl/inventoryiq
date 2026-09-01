package com.inventoryiq.adapters.in.rest.dto;

import com.inventoryiq.domain.model.RecommendationStatus;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de la request de PATCH /api/v1/recommendations/{recommendationId} (Sección 8.7). */
public record RegisterRecommendationFeedbackRequest(@NotNull RecommendationStatus status, String comment) {
}

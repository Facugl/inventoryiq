package com.inventoryiq.adapters.in.rest.dto;

/** Forma JSON pública del resumen de ejecución de POST /api/v1/recommendations/recalculate (Sección 8.6). */
public record RecalculateRecommendationsResponse(
		int totalGenerated,
		int newCount,
		int updatedCount,
		int autoDiscardedCount) {
}

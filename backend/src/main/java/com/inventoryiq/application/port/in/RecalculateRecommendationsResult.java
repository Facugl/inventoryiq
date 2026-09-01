package com.inventoryiq.application.port.in;

/** Resumen de ejecución de RecalculateRecommendationsUseCase (Sección 8.6). */
public record RecalculateRecommendationsResult(
		int totalGenerated,
		int newCount,
		int updatedCount,
		int autoDiscardedCount) {
}

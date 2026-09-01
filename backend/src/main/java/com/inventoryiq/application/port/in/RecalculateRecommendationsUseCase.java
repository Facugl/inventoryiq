package com.inventoryiq.application.port.in;

/** Puerto de entrada — Sección 8.6. */
public interface RecalculateRecommendationsUseCase {

	RecalculateRecommendationsResult execute(RecalculateRecommendationsCommand command);
}

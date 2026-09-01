package com.inventoryiq.application.port.in;

/** Puerto de entrada — Sección 8.7/9.8. Lanza RecommendationNotFoundException si recommendationId no existe. */
public interface RegisterRecommendationFeedbackUseCase {

	RecommendationResult execute(RegisterRecommendationFeedbackCommand command);
}

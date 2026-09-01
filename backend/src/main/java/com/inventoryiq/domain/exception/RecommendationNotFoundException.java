package com.inventoryiq.domain.exception;

/** Se lanza cuando se busca una recomendación por id y no existe (Sección 8.7). */
public class RecommendationNotFoundException extends NotFoundException {
	public RecommendationNotFoundException(Long recommendationId) {
		super("Recommendation not found: " + recommendationId);
	}
}

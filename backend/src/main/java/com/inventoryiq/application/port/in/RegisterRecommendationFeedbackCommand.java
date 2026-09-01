package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.RecommendationStatus;

import java.time.LocalDate;

/**
 * Parámetros de entrada de RegisterRecommendationFeedbackUseCase
 * (Sección 8.7/9.8).
 *
 * newStatus solo se valida acá como no-nulo: si vale PENDING o si la
 * recomendación ya estaba resuelta, lo rechaza Recommendation.withFeedback
 * (única fuente de verdad de esa regla, no duplicada acá).
 *
 * feedbackDate no aparece en el body documentado por la Sección 8.7: la
 * resuelve el adaptador REST con la fecha del sistema, mismo criterio que
 * RecalculateRecommendationsCommand.referenceDate.
 */
public record RegisterRecommendationFeedbackCommand(
		Long recommendationId,
		RecommendationStatus newStatus,
		String comment,
		LocalDate feedbackDate) {

	public RegisterRecommendationFeedbackCommand {
		if (recommendationId == null) {
			throw new InvalidDomainDataException("recommendationId is required");
		}

		if (newStatus == null) {
			throw new InvalidDomainDataException("newStatus is required");
		}

		if (feedbackDate == null) {
			throw new InvalidDomainDataException("feedbackDate is required; the use case never reads the system clock");
		}
	}
}

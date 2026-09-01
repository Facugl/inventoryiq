package com.inventoryiq.domain.model;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Sección 8.5/8.6/8.7 — Recomendación de compra persistida. A diferencia
 * de ReorderSuggestionResult (Sección 9.2, cálculo efímero sin guardar),
 * esta es la versión con identidad y ciclo de vida: se crea al recalcular
 * (8.6), se lista por estado (8.5) y recibe feedback del comprador (8.7),
 * insumo del KPI "% de recomendaciones seguidas" (Sección 3.4).
 *
 * recommendationId es null antes de persistir por primera vez (lo asigna
 * la base de datos al insertar). feedbackComment y feedbackDate son null
 * mientras status es PENDING.
 */
public record Recommendation(
		Long recommendationId,
		Long productId,
		Long storeId,
		Long supplierId,
		int suggestedQuantity,
		LocalDate orderDeadlineDate,
		String justification,
		RecommendationStatus status,
		LocalDate generationDate,
		String feedbackComment,
		LocalDate feedbackDate) {

	public Recommendation {
		if (suggestedQuantity < 0) {
			throw new InvalidDomainDataException("Suggested quantity cannot be negative, received: " + suggestedQuantity);
		}
	}

	/**
	 * Sección 8.7 — feedback del comprador. Solo una recomendación PENDING
	 * puede recibir feedback (una ya resuelta no se puede volver a marcar),
	 * y el nuevo estado tiene que ser APPLIED o DISCARDED (PENDING es el
	 * estado inicial que asigna el sistema, no un valor de feedback válido).
	 */
	public Recommendation withFeedback(RecommendationStatus newStatus, String comment, LocalDate feedbackDate) {
		if (status != RecommendationStatus.PENDING) {
			throw new InvalidDomainDataException(
					"Only a pending recommendation can receive feedback, current status: " + status);
		}
		if (newStatus != RecommendationStatus.APPLIED && newStatus != RecommendationStatus.DISCARDED) {
			throw new InvalidDomainDataException(
					"Feedback status must be APPLIED or DISCARDED, received: " + newStatus);
		}

		return new Recommendation(recommendationId, productId, storeId, supplierId, suggestedQuantity,
				orderDeadlineDate, justification, newStatus, generationDate, comment, feedbackDate);
	}
}

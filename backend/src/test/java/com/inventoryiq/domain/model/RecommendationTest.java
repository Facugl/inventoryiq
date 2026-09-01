package com.inventoryiq.domain.model;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecommendationTest {

	private static final LocalDate GENERATION_DATE = LocalDate.parse("2026-08-01");
	private static final LocalDate FEEDBACK_DATE = LocalDate.parse("2026-08-03");

	@Test
	void suggestedQuantityCannotBeNegative() {
		assertThrows(InvalidDomainDataException.class, () -> new Recommendation(
				1L, 1001L, 1L, 5L, -1, GENERATION_DATE.plusDays(2), "justificación",
				RecommendationStatus.PENDING, GENERATION_DATE, null, null));
	}

	@Test
	void aPendingRecommendationCanBeMarkedAsApplied() {
		Recommendation updated = pending().withFeedback(RecommendationStatus.APPLIED, "comprado", FEEDBACK_DATE);

		assertEquals(RecommendationStatus.APPLIED, updated.status());
		assertEquals("comprado", updated.feedbackComment());
		assertEquals(FEEDBACK_DATE, updated.feedbackDate());
	}

	@Test
	void aPendingRecommendationCanBeMarkedAsDiscarded() {
		Recommendation updated = pending().withFeedback(RecommendationStatus.DISCARDED, null, FEEDBACK_DATE);

		assertEquals(RecommendationStatus.DISCARDED, updated.status());
	}

	@Test
	void anAlreadyResolvedRecommendationCannotReceiveFeedbackAgain() {
		Recommendation applied = pending().withFeedback(RecommendationStatus.APPLIED, "comprado", FEEDBACK_DATE);

		assertThrows(InvalidDomainDataException.class,
				() -> applied.withFeedback(RecommendationStatus.DISCARDED, "cambié de idea", FEEDBACK_DATE));
	}

	@Test
	void feedbackStatusCannotBePending() {
		assertThrows(InvalidDomainDataException.class,
				() -> pending().withFeedback(RecommendationStatus.PENDING, null, FEEDBACK_DATE));
	}

	private static Recommendation pending() {
		return new Recommendation(1L, 1001L, 1L, 5L, 100, GENERATION_DATE.plusDays(2), "justificación",
				RecommendationStatus.PENDING, GENERATION_DATE, null, null);
	}
}

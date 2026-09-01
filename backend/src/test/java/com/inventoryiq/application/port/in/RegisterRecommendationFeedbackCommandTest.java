package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.RecommendationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RegisterRecommendationFeedbackCommandTest {

	@Test
	void recommendationIdIsRequired() {
		assertThrows(InvalidDomainDataException.class, () -> new RegisterRecommendationFeedbackCommand(
				null, RecommendationStatus.APPLIED, null, LocalDate.now()));
	}

	@Test
	void newStatusIsRequired() {
		assertThrows(InvalidDomainDataException.class, () -> new RegisterRecommendationFeedbackCommand(
				1L, null, null, LocalDate.now()));
	}

	@Test
	void feedbackDateIsRequired() {
		assertThrows(InvalidDomainDataException.class, () -> new RegisterRecommendationFeedbackCommand(
				1L, RecommendationStatus.APPLIED, null, null));
	}
}

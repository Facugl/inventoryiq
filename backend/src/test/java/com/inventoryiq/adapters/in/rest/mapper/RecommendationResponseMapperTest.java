package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.RecommendationResponse;
import com.inventoryiq.application.port.in.RecommendationResult;
import com.inventoryiq.domain.model.RecommendationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationResponseMapperTest {

	@Test
	void mapsEveryField() {
		RecommendationResult result = new RecommendationResult(
				1L, 1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L, 5L, 100,
				LocalDate.parse("2026-08-05"), "justificación de prueba", RecommendationStatus.APPLIED,
				LocalDate.parse("2026-08-01"), "comprado", LocalDate.parse("2026-08-03"));

		RecommendationResponse response = RecommendationResponseMapper.toResponse(result);

		assertEquals(1L, response.recommendationId());
		assertEquals(1001L, response.productId());
		assertEquals("LEC-1001", response.sku());
		assertEquals("Leche Entera 1L", response.productName());
		assertEquals(1L, response.storeId());
		assertEquals(2L, response.categoryId());
		assertEquals(5L, response.supplierId());
		assertEquals(100, response.suggestedQuantity());
		assertEquals(LocalDate.parse("2026-08-05"), response.orderDeadlineDate());
		assertEquals("justificación de prueba", response.justification());
		assertEquals(RecommendationStatus.APPLIED, response.status());
		assertEquals(LocalDate.parse("2026-08-01"), response.generationDate());
		assertEquals("comprado", response.feedbackComment());
		assertEquals(LocalDate.parse("2026-08-03"), response.feedbackDate());
	}
}

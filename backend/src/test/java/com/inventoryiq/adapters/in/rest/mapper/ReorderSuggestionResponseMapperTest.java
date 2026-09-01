package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.ReorderSuggestionResponse;
import com.inventoryiq.application.port.in.ReorderSuggestionResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReorderSuggestionResponseMapperTest {

	@Test
	void mapsEveryField() {
		ReorderSuggestionResult result = new ReorderSuggestionResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L, 5L,
				100, LocalDate.parse("2026-08-03"), "justificación de prueba");

		ReorderSuggestionResponse response = ReorderSuggestionResponseMapper.toResponse(result);

		assertEquals(1001L, response.productId());
		assertEquals("LEC-1001", response.sku());
		assertEquals("Leche Entera 1L", response.productName());
		assertEquals(1L, response.storeId());
		assertEquals(2L, response.categoryId());
		assertEquals(5L, response.supplierId());
		assertEquals(100, response.suggestedQuantity());
		assertEquals(LocalDate.parse("2026-08-03"), response.orderDeadlineDate());
		assertEquals("justificación de prueba", response.justification());
	}
}

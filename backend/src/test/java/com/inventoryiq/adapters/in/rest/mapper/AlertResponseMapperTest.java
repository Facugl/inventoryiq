package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.AlertResponse;
import com.inventoryiq.application.port.in.AlertResult;
import com.inventoryiq.application.port.in.AlertSeverity;
import com.inventoryiq.application.port.in.AlertType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertResponseMapperTest {

	@Test
	void mapsEveryField() {
		AlertResult result = new AlertResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L,
				AlertType.STOCKOUT, AlertSeverity.HIGH, LocalDate.parse("2026-08-01"));

		AlertResponse response = AlertResponseMapper.toResponse(result);

		assertEquals(1001L, response.productId());
		assertEquals("LEC-1001", response.sku());
		assertEquals("Leche Entera 1L", response.productName());
		assertEquals(1L, response.storeId());
		assertEquals(2L, response.categoryId());
		assertEquals(AlertType.STOCKOUT, response.type());
		assertEquals(AlertSeverity.HIGH, response.severity());
		assertEquals(LocalDate.parse("2026-08-01"), response.generatedAt());
	}
}

package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.OverstockProductResponse;
import com.inventoryiq.application.port.in.OverstockProductResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverstockProductResponseMapperTest {

	@Test
	void mapsEveryField() {
		OverstockProductResult result = new OverstockProductResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L,
				100, 45.5, new BigDecimal("45000.00"));

		OverstockProductResponse response = OverstockProductResponseMapper.toResponse(result);

		assertEquals(1001L, response.productId());
		assertEquals("LEC-1001", response.sku());
		assertEquals("Leche Entera 1L", response.productName());
		assertEquals(1L, response.storeId());
		assertEquals(2L, response.categoryId());
		assertEquals(100, response.currentStock());
		assertEquals(45.5, response.currentDaysOfCoverage());
		assertEquals(new BigDecimal("45000.00"), response.immobilizedValue());
	}
}

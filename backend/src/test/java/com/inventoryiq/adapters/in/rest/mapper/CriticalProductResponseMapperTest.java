package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.CriticalProductResponse;
import com.inventoryiq.application.port.in.CriticalProductResult;
import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.vo.CriticalityLevel;
import com.inventoryiq.domain.model.vo.ReorderPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CriticalProductResponseMapperTest {

	@Test
	void mapsEveryFieldAndFlattensValueObjectsToPlainNumbers() {
		CriticalProductResult result = new CriticalProductResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L,
				50, new ReorderPoint(60.0), 5.0,
				ProductStatus.REQUIRES_REPLENISHMENT, new CriticalityLevel(33.33));

		CriticalProductResponse response = CriticalProductResponseMapper.toResponse(result);

		assertEquals(1001L, response.productId());
		assertEquals("LEC-1001", response.sku());
		assertEquals("Leche Entera 1L", response.productName());
		assertEquals(1L, response.storeId());
		assertEquals(2L, response.categoryId());
		assertEquals(50, response.currentStock());
		assertEquals(60.0, response.reorderPointUnits());
		assertEquals(5.0, response.currentDaysOfCoverage());
		assertEquals(ProductStatus.REQUIRES_REPLENISHMENT, response.status());
		assertEquals(33.33, response.criticalityScore());
	}
}

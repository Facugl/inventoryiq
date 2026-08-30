package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.ProductClassificationResponse;
import com.inventoryiq.application.port.in.ProductClassificationResult;
import com.inventoryiq.domain.model.AbcClassification;
import com.inventoryiq.domain.model.XyzClassification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductClassificationResponseMapperTest {

	@Test
	void mapsEveryField() {
		ProductClassificationResult result = new ProductClassificationResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L,
				AbcClassification.A, XyzClassification.X);

		ProductClassificationResponse response = ProductClassificationResponseMapper.toResponse(result);

		assertEquals(1001L, response.productId());
		assertEquals("LEC-1001", response.sku());
		assertEquals("Leche Entera 1L", response.productName());
		assertEquals(1L, response.storeId());
		assertEquals(2L, response.categoryId());
		assertEquals(AbcClassification.A, response.abcClass());
		assertEquals(XyzClassification.X, response.xyzClass());
	}
}

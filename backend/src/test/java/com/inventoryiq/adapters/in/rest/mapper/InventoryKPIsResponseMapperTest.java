package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.InventoryKPIsResponse;
import com.inventoryiq.application.port.in.InventoryKPIsResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryKPIsResponseMapperTest {

	@Test
	void mapsEveryField() {
		InventoryKPIsResult result = new InventoryKPIsResult(12.5, 8.3, new BigDecimal("1500.00"), 66.67, 3.2);

		InventoryKPIsResponse response = InventoryKPIsResponseMapper.toResponse(result);

		assertEquals(12.5, response.stockoutRate());
		assertEquals(8.3, response.averageDaysOfCoverage());
		assertEquals(new BigDecimal("1500.00"), response.immobilizedOverstockValue());
		assertEquals(66.67, response.recommendationsFollowedRate());
		assertEquals(3.2, response.inventoryTurnover());
	}

	@Test
	void mapsNullFieldsWhenThereIsNotEnoughData() {
		InventoryKPIsResult result = new InventoryKPIsResult(null, null, BigDecimal.ZERO, null, null);

		InventoryKPIsResponse response = InventoryKPIsResponseMapper.toResponse(result);

		assertNull(response.stockoutRate());
		assertNull(response.averageDaysOfCoverage());
		assertEquals(BigDecimal.ZERO, response.immobilizedOverstockValue());
		assertNull(response.recommendationsFollowedRate());
		assertNull(response.inventoryTurnover());
	}
}

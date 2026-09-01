package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.RecalculateProductStatusResponse;
import com.inventoryiq.application.port.in.RecalculateProductStatusResult;
import com.inventoryiq.application.port.in.RecalculateRecommendationsResult;
import com.inventoryiq.application.port.in.StoreRecalculationSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecalculateProductStatusResponseMapperTest {

	@Test
	void mapsEveryFieldIncludingNestedRecommendationsSummary() {
		RecalculateProductStatusResult result = new RecalculateProductStatusResult(1, List.of(
				new StoreRecalculationSummary(1L, 3, 2, 4, new RecalculateRecommendationsResult(5, 3, 2, 1))));

		RecalculateProductStatusResponse response = RecalculateProductStatusResponseMapper.toResponse(result);

		assertEquals(1, response.storesProcessed());
		assertEquals(1, response.perStore().size());
		var storeResponse = response.perStore().get(0);
		assertEquals(1L, storeResponse.storeId());
		assertEquals(3, storeResponse.criticalProductsFound());
		assertEquals(2, storeResponse.overstockProductsFound());
		assertEquals(4, storeResponse.alertsGenerated());
		assertEquals(5, storeResponse.recommendations().totalGenerated());
		assertEquals(3, storeResponse.recommendations().newCount());
		assertEquals(2, storeResponse.recommendations().updatedCount());
		assertEquals(1, storeResponse.recommendations().autoDiscardedCount());
	}
}

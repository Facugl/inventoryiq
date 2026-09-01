package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.ForecastDemandResponse;
import com.inventoryiq.application.port.in.DemandForecastPeriod;
import com.inventoryiq.application.port.in.ForecastDemandResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForecastDemandResponseMapperTest {

	@Test
	void mapsEveryFieldIncludingPeriods() {
		ForecastDemandResult result = new ForecastDemandResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 15.0,
				List.of(new DemandForecastPeriod(
						LocalDate.parse("2025-12-25"), LocalDate.parse("2025-12-31"), 1.333333, 20.0, 140)));

		ForecastDemandResponse response = ForecastDemandResponseMapper.toResponse(result);

		assertEquals(1001L, response.productId());
		assertEquals("LEC-1001", response.sku());
		assertEquals("Leche Entera 1L", response.productName());
		assertEquals(1L, response.storeId());
		assertEquals(15.0, response.baseAds());
		assertEquals(1, response.periods().size());
		assertEquals(LocalDate.parse("2025-12-25"), response.periods().get(0).periodStart());
		assertEquals(LocalDate.parse("2025-12-31"), response.periods().get(0).periodEnd());
		assertEquals(1.333333, response.periods().get(0).seasonalIndex());
		assertEquals(20.0, response.periods().get(0).projectedDailyAds());
		assertEquals(140, response.periods().get(0).projectedTotalDemand());
	}

	@Test
	void mapsANullBaseAdsAndEmptyPeriodsWhenThereIsNoHistory() {
		ForecastDemandResult result = new ForecastDemandResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, null, List.of());

		ForecastDemandResponse response = ForecastDemandResponseMapper.toResponse(result);

		assertEquals(null, response.baseAds());
		assertEquals(0, response.periods().size());
	}
}

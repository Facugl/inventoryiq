package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.DemandForecastPeriod;
import com.inventoryiq.application.port.in.ForecastDemandQuery;
import com.inventoryiq.application.port.in.ForecastDemandResult;
import com.inventoryiq.application.port.in.ForecastDemandUseCase;
import com.inventoryiq.domain.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el controller de forma aislada: ForecastDemandUseCase está
 * mockeado, no hay CSV ni casos de uso reales de por medio.
 */
@WebMvcTest(ForecastController.class)
class ForecastControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ForecastDemandUseCase forecastDemandUseCase;

	@Test
	void returnsAForecastAsJsonForAValidRequest() throws Exception {
		ForecastDemandResult result = new ForecastDemandResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 15.0,
				List.of(new DemandForecastPeriod(
						LocalDate.parse("2025-12-25"), LocalDate.parse("2025-12-31"), 1.333333, 20.0, 140)));
		given(forecastDemandUseCase.execute(any(ForecastDemandQuery.class))).willReturn(result);

		mockMvc.perform(get("/api/v1/products/1001/forecast")
						.param("storeId", "1")
						.param("referenceDate", "2025-12-24")
						.param("horizonDays", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(1001))
				.andExpect(jsonPath("$.sku").value("LEC-1001"))
				.andExpect(jsonPath("$.baseAds").value(15.0))
				.andExpect(jsonPath("$.periods[0].periodStart").value("2025-12-25"))
				.andExpect(jsonPath("$.periods[0].periodEnd").value("2025-12-31"))
				.andExpect(jsonPath("$.periods[0].projectedTotalDemand").value(140));
	}

	@Test
	void returns404WhenTheProductDoesNotExist() throws Exception {
		given(forecastDemandUseCase.execute(any(ForecastDemandQuery.class)))
				.willThrow(new ProductNotFoundException(9999L));

		mockMvc.perform(get("/api/v1/products/9999/forecast")
						.param("storeId", "1")
						.param("referenceDate", "2025-12-24")
						.param("horizonDays", "7"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void returns400WhenStoreIdIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/products/1001/forecast")
						.param("referenceDate", "2025-12-24")
						.param("horizonDays", "7"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenReferenceDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/products/1001/forecast")
						.param("storeId", "1")
						.param("horizonDays", "7"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenHorizonDaysIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/products/1001/forecast")
						.param("storeId", "1")
						.param("referenceDate", "2025-12-24"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenHorizonDaysIsZeroOrNegative() throws Exception {
		mockMvc.perform(get("/api/v1/products/1001/forecast")
						.param("storeId", "1")
						.param("referenceDate", "2025-12-24")
						.param("horizonDays", "0"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenProductIdIsNotNumeric() throws Exception {
		mockMvc.perform(get("/api/v1/products/abc/forecast")
						.param("storeId", "1")
						.param("referenceDate", "2025-12-24")
						.param("horizonDays", "7"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenStoreIdIsZeroOrNegative() throws Exception {
		mockMvc.perform(get("/api/v1/products/1001/forecast")
						.param("storeId", "0")
						.param("referenceDate", "2025-12-24")
						.param("horizonDays", "7"))
				.andExpect(status().isBadRequest());
	}
}

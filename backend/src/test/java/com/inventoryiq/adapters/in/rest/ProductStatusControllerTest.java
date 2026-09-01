package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.RecalculateProductStatusCommand;
import com.inventoryiq.application.port.in.RecalculateProductStatusResult;
import com.inventoryiq.application.port.in.RecalculateProductStatusUseCase;
import com.inventoryiq.application.port.in.RecalculateRecommendationsResult;
import com.inventoryiq.application.port.in.StoreRecalculationSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el controller de forma aislada: RecalculateProductStatusUseCase
 * está mockeado, no hay CSV ni Postgres de por medio.
 */
@WebMvcTest(ProductStatusController.class)
@Import(ProductStatusControllerTest.FixedClockConfig.class)
class ProductStatusControllerTest {

	@TestConfiguration
	static class FixedClockConfig {
		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-02-01T00:00:00Z"), ZoneOffset.UTC);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RecalculateProductStatusUseCase recalculateProductStatusUseCase;

	@Test
	void recalculatesForASpecificStoreWhenStoreIdIsProvided() throws Exception {
		RecalculateProductStatusResult result = new RecalculateProductStatusResult(1, List.of(
				new StoreRecalculationSummary(1L, 3, 2, 4, new RecalculateRecommendationsResult(5, 3, 2, 1))));
		given(recalculateProductStatusUseCase.execute(any(RecalculateProductStatusCommand.class))).willReturn(result);

		mockMvc.perform(post("/api/v1/product-status/recalculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"storeId\": 1}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.storesProcessed").value(1))
				.andExpect(jsonPath("$.perStore[0].storeId").value(1))
				.andExpect(jsonPath("$.perStore[0].criticalProductsFound").value(3))
				.andExpect(jsonPath("$.perStore[0].recommendations.totalGenerated").value(5));
	}

	@Test
	void recalculatesForAllStoresWhenTheBodyIsOmitted() throws Exception {
		given(recalculateProductStatusUseCase.execute(any(RecalculateProductStatusCommand.class)))
				.willReturn(new RecalculateProductStatusResult(2, List.of()));

		mockMvc.perform(post("/api/v1/product-status/recalculate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.storesProcessed").value(2));
	}

	@Test
	void returns400WhenStoreIdIsZeroOrNegative() throws Exception {
		mockMvc.perform(post("/api/v1/product-status/recalculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"storeId\": 0}"))
				.andExpect(status().isBadRequest());
	}
}

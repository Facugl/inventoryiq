package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.CalculateInventoryKPIsQuery;
import com.inventoryiq.application.port.in.CalculateInventoryKPIsUseCase;
import com.inventoryiq.application.port.in.InventoryKPIsResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el controller de forma aislada: CalculateInventoryKPIsUseCase
 * está mockeado, no hay CSV ni Postgres de por medio.
 */
@WebMvcTest(KPIsController.class)
class KPIsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CalculateInventoryKPIsUseCase calculateInventoryKPIsUseCase;

	@Test
	void returnsKPIsAsJsonForAValidRequest() throws Exception {
		InventoryKPIsResult result = new InventoryKPIsResult(12.5, 8.3, new BigDecimal("1500.00"), 66.67, 3.2);
		given(calculateInventoryKPIsUseCase.execute(any(CalculateInventoryKPIsQuery.class))).willReturn(result);

		mockMvc.perform(get("/api/v1/kpis")
						.param("storeId", "1")
						.param("fromDate", "2026-07-01")
						.param("toDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stockoutRate").value(12.5))
				.andExpect(jsonPath("$.averageDaysOfCoverage").value(8.3))
				.andExpect(jsonPath("$.immobilizedOverstockValue").value(1500.00))
				.andExpect(jsonPath("$.recommendationsFollowedRate").value(66.67))
				.andExpect(jsonPath("$.inventoryTurnover").value(3.2));
	}

	@Test
	void returns400WhenStoreIdIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/kpis")
						.param("fromDate", "2026-07-01")
						.param("toDate", "2026-08-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenFromDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/kpis")
						.param("storeId", "1")
						.param("toDate", "2026-08-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenToDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/kpis")
						.param("storeId", "1")
						.param("fromDate", "2026-07-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenStoreIdIsZeroOrNegative() throws Exception {
		mockMvc.perform(get("/api/v1/kpis")
						.param("storeId", "0")
						.param("fromDate", "2026-07-01")
						.param("toDate", "2026-08-01"))
				.andExpect(status().isBadRequest());
	}
}

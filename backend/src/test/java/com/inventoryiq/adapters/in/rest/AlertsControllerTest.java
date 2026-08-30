package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.AlertResult;
import com.inventoryiq.application.port.in.AlertSeverity;
import com.inventoryiq.application.port.in.AlertType;
import com.inventoryiq.application.port.in.GenerateAlertsQuery;
import com.inventoryiq.application.port.in.GenerateAlertsUseCase;
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
 * Prueba el controller de forma aislada: GenerateAlertsUseCase está
 * mockeado, no hay CSV ni casos de uso reales de por medio.
 */
@WebMvcTest(AlertsController.class)
class AlertsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GenerateAlertsUseCase generateAlertsUseCase;

	@Test
	void returnsAlertsAsJsonForAValidRequest() throws Exception {
		AlertResult result = new AlertResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L,
				AlertType.STOCKOUT, AlertSeverity.HIGH, LocalDate.parse("2026-08-01"));
		given(generateAlertsUseCase.execute(any(GenerateAlertsQuery.class)))
				.willReturn(List.of(result));

		mockMvc.perform(get("/api/v1/alerts")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(1001))
				.andExpect(jsonPath("$[0].sku").value("LEC-1001"))
				.andExpect(jsonPath("$[0].productName").value("Leche Entera 1L"))
				.andExpect(jsonPath("$[0].storeId").value(1))
				.andExpect(jsonPath("$[0].categoryId").value(2))
				.andExpect(jsonPath("$[0].type").value("STOCKOUT"))
				.andExpect(jsonPath("$[0].severity").value("HIGH"))
				.andExpect(jsonPath("$[0].generatedAt").value("2026-08-01"));
	}

	@Test
	void acceptsOptionalTypeAndSeverityFilters() throws Exception {
		given(generateAlertsUseCase.execute(any(GenerateAlertsQuery.class)))
				.willReturn(List.of());

		mockMvc.perform(get("/api/v1/alerts")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("type", "OVERSTOCK")
						.param("severity", "MEDIUM"))
				.andExpect(status().isOk());
	}

	@Test
	void returns400WhenStoreIdIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/alerts")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void returns400WhenReferenceDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/alerts")
						.param("storeId", "1"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenStoreIdIsZeroOrNegative() throws Exception {
		mockMvc.perform(get("/api/v1/alerts")
						.param("storeId", "0")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenTypeIsNotAValidValue() throws Exception {
		mockMvc.perform(get("/api/v1/alerts")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("type", "not_a_valid_value"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenReferenceDateIsNotAValidDate() throws Exception {
		mockMvc.perform(get("/api/v1/alerts")
						.param("storeId", "1")
						.param("referenceDate", "not-a-date"))
				.andExpect(status().isBadRequest());
	}
}

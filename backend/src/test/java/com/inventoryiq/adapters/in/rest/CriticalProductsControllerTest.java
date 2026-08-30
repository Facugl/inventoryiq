package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.CriticalProductResult;
import com.inventoryiq.application.port.in.GetCriticalProductsQuery;
import com.inventoryiq.application.port.in.GetCriticalProductsUseCase;
import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.vo.CriticalityLevel;
import com.inventoryiq.domain.model.vo.ReorderPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el controller de forma aislada: GetCriticalProductsUseCase está
 * mockeado, no hay CSV ni casos de uso reales de por medio. Verifica el
 * binding/validación de query params y el mapeo a JSON.
 */
@WebMvcTest(CriticalProductsController.class)
class CriticalProductsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetCriticalProductsUseCase getCriticalProductsUseCase;

	@Test
	void returnsCriticalProductsAsJsonForAValidRequest() throws Exception {
		CriticalProductResult result = new CriticalProductResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L,
				50, new ReorderPoint(60.0), 5.0,
				ProductStatus.REQUIRES_REPLENISHMENT, new CriticalityLevel(33.33));
		given(getCriticalProductsUseCase.execute(any(GetCriticalProductsQuery.class)))
				.willReturn(List.of(result));

		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(1001))
				.andExpect(jsonPath("$[0].sku").value("LEC-1001"))
				.andExpect(jsonPath("$[0].productName").value("Leche Entera 1L"))
				.andExpect(jsonPath("$[0].storeId").value(1))
				.andExpect(jsonPath("$[0].categoryId").value(2))
				.andExpect(jsonPath("$[0].currentStock").value(50))
				.andExpect(jsonPath("$[0].reorderPointUnits").value(60.0))
				.andExpect(jsonPath("$[0].currentDaysOfCoverage").value(5.0))
				.andExpect(jsonPath("$[0].status").value("REQUIRES_REPLENISHMENT"))
				.andExpect(jsonPath("$[0].criticalityScore").value(33.33));
	}

	@Test
	void acceptsOptionalCategoryIdAndLimit() throws Exception {
		given(getCriticalProductsUseCase.execute(any(GetCriticalProductsQuery.class)))
				.willReturn(List.of());

		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "1")
						.param("categoryId", "2")
						.param("limit", "5")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk());
	}

	@Test
	void returns400WhenStoreIdIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void returns400WhenReferenceDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void returns400WhenLimitIsZeroOrNegative() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("limit", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());

		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("limit", "-1"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenLimitIsNotNumeric() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("limit", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void returns400WhenReferenceDateIsNotAValidDate() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "1")
						.param("referenceDate", "not-a-date"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void returns400WhenStoreIdIsNotNumeric() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "abc")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void returns400WhenStoreIdIsZeroOrNegative() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "0")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "-1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenCategoryIdIsNotNumeric() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("categoryId", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void returns400WhenCategoryIdIsZeroOrNegative() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("categoryId", "0"))
				.andExpect(status().isBadRequest());
	}
}

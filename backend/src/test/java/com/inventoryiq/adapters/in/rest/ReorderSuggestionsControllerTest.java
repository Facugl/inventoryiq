package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.GenerateReorderSuggestionsQuery;
import com.inventoryiq.application.port.in.GenerateReorderSuggestionsUseCase;
import com.inventoryiq.application.port.in.ReorderSuggestionResult;
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
 * Prueba el controller de forma aislada: GenerateReorderSuggestionsUseCase
 * está mockeado, no hay CSV ni casos de uso reales de por medio.
 */
@WebMvcTest(ReorderSuggestionsController.class)
class ReorderSuggestionsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GenerateReorderSuggestionsUseCase generateReorderSuggestionsUseCase;

	@Test
	void returnsSuggestionsAsJsonForAValidRequest() throws Exception {
		ReorderSuggestionResult result = new ReorderSuggestionResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L, 5L,
				100, LocalDate.parse("2026-08-03"), "justificación de prueba");
		given(generateReorderSuggestionsUseCase.execute(any(GenerateReorderSuggestionsQuery.class)))
				.willReturn(List.of(result));

		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(1001))
				.andExpect(jsonPath("$[0].sku").value("LEC-1001"))
				.andExpect(jsonPath("$[0].productName").value("Leche Entera 1L"))
				.andExpect(jsonPath("$[0].storeId").value(1))
				.andExpect(jsonPath("$[0].categoryId").value(2))
				.andExpect(jsonPath("$[0].supplierId").value(5))
				.andExpect(jsonPath("$[0].suggestedQuantity").value(100))
				.andExpect(jsonPath("$[0].orderDeadlineDate").value("2026-08-03"))
				.andExpect(jsonPath("$[0].justification").value("justificación de prueba"));
	}

	@Test
	void acceptsOptionalCategoryIdAndSupplierId() throws Exception {
		given(generateReorderSuggestionsUseCase.execute(any(GenerateReorderSuggestionsQuery.class)))
				.willReturn(List.of());

		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("storeId", "1")
						.param("categoryId", "2")
						.param("supplierId", "5")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk());
	}

	@Test
	void returns400WhenStoreIdIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void returns400WhenReferenceDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("storeId", "1"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenStoreIdIsZeroOrNegative() throws Exception {
		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("storeId", "0")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenSupplierIdIsNotNumeric() throws Exception {
		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("supplierId", "abc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenReferenceDateIsNotAValidDate() throws Exception {
		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("storeId", "1")
						.param("referenceDate", "not-a-date"))
				.andExpect(status().isBadRequest());
	}
}

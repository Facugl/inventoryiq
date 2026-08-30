package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.ClassifyProductsQuery;
import com.inventoryiq.application.port.in.ClassifyProductsUseCase;
import com.inventoryiq.application.port.in.ProductClassificationResult;
import com.inventoryiq.domain.model.AbcClassification;
import com.inventoryiq.domain.model.XyzClassification;
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
 * Prueba el controller de forma aislada: ClassifyProductsUseCase está
 * mockeado, no hay CSV ni casos de uso reales de por medio.
 */
@WebMvcTest(ProductClassificationController.class)
class ProductClassificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ClassifyProductsUseCase classifyProductsUseCase;

	@Test
	void returnsClassificationAsJsonForAValidRequest() throws Exception {
		ProductClassificationResult result = new ProductClassificationResult(
				1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L, AbcClassification.A, XyzClassification.X);
		given(classifyProductsUseCase.execute(any(ClassifyProductsQuery.class)))
				.willReturn(List.of(result));

		mockMvc.perform(get("/api/v1/products/classification")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(1001))
				.andExpect(jsonPath("$[0].sku").value("LEC-1001"))
				.andExpect(jsonPath("$[0].productName").value("Leche Entera 1L"))
				.andExpect(jsonPath("$[0].storeId").value(1))
				.andExpect(jsonPath("$[0].categoryId").value(2))
				.andExpect(jsonPath("$[0].abcClass").value("A"))
				.andExpect(jsonPath("$[0].xyzClass").value("X"));
	}

	@Test
	void acceptsOptionalCategoryId() throws Exception {
		given(classifyProductsUseCase.execute(any(ClassifyProductsQuery.class)))
				.willReturn(List.of());

		mockMvc.perform(get("/api/v1/products/classification")
						.param("storeId", "1")
						.param("categoryId", "2")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk());
	}

	@Test
	void returns400WhenStoreIdIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/products/classification")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void returns400WhenReferenceDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/products/classification")
						.param("storeId", "1"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenStoreIdIsZeroOrNegative() throws Exception {
		mockMvc.perform(get("/api/v1/products/classification")
						.param("storeId", "0")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/products/classification")
						.param("storeId", "-1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenCategoryIdIsNotNumeric() throws Exception {
		mockMvc.perform(get("/api/v1/products/classification")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("categoryId", "abc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns400WhenReferenceDateIsNotAValidDate() throws Exception {
		mockMvc.perform(get("/api/v1/products/classification")
						.param("storeId", "1")
						.param("referenceDate", "not-a-date"))
				.andExpect(status().isBadRequest());
	}
}

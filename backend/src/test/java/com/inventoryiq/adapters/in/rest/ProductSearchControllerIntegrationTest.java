package com.inventoryiq.adapters.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * De punta a punta: HTTP -> ProductSearchController -> SearchProductsUseCase
 * (wiring real de config/) -> CsvProductRepositoryAdapter real ->
 * productos.csv real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class ProductSearchControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void findsAKnownProductByCodeCaseInsensitive() throws Exception {
		mockMvc.perform(get("/api/v1/products").param("q", "que-1008"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId == 1008)].name").value(contains("Queso Cremoso 300g")));
	}

	@Test
	void findsAKnownProductByNameSubstring() throws Exception {
		mockMvc.perform(get("/api/v1/products").param("q", "empanadas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId == 1051)].sku").value(contains("EMP-1051")));
	}

	@Test
	void rejectsBlankSearchTerm() throws Exception {
		mockMvc.perform(get("/api/v1/products").param("q", "  "))
				.andExpect(status().isBadRequest());
	}
}

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
 * De punta a punta: HTTP -> CategoriesController -> ListCategoriesUseCase
 * (wiring real de config/) -> CsvCategoryRepositoryAdapter real ->
 * categorias.csv real (14 categorías — ver docs/README_datos_simulados.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class CategoriesControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsAllCategoriesFromTheRealCsvIncludingRootAndChild() throws Exception {
		mockMvc.perform(get("/api/v1/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(14))
				.andExpect(jsonPath("$[?(@.categoryId == 1)].name").value(contains("Almacén Frío")))
				.andExpect(jsonPath("$[?(@.categoryId == 2)].name").value(contains("Lácteos")))
				.andExpect(jsonPath("$[?(@.categoryId == 2)].parentCategoryId").value(contains(1)))
				.andExpect(jsonPath("$[?(@.categoryId == 2)].maxCoverageDaysThreshold").value(contains(12)))
				.andExpect(jsonPath("$[?(@.categoryId == 2)].defaultExtraCoverageDays").value(contains(3)));
	}
}

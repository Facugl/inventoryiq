package com.inventoryiq.adapters.in.rest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * De punta a punta: HTTP -> CategoriesController -> UpdateCategoryParametersUseCase
 * (wiring real de config/) -> adaptador CSV real -> un CSV aislado (copia de
 * src/test/resources/csv-fixtures en un @TempDir, nunca data/csv real) —
 * mismo criterio que SuppliersControllerIntegrationTest. Separada de
 * CategoriesControllerIntegrationTest porque esa clase lee directamente
 * data/csv/categorias.csv (14 categorías reales) sin @TempDir — mezclar un
 * test que escribe en esa misma clase mutaría datos reales del repo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class CategoryParametersControllerIntegrationTest {

	@TempDir
	static Path tempCsvDir;

	@DynamicPropertySource
	static void csvBasePath(DynamicPropertyRegistry registry) {
		registry.add("inventoryiq.csv.base-path", () -> tempCsvDir.toString());
	}

	@BeforeAll
	static void copyFixtures() throws IOException {
		Path fixtures = Path.of("src/test/resources/csv-fixtures");
		try (Stream<Path> files = Files.list(fixtures)) {
			for (Path file : files.toList()) {
				Files.copy(file, tempCsvDir.resolve(file.getFileName()));
			}
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void updatesTheParametersAndPersistsThemThroughTheRealWiring() throws Exception {
		mockMvc.perform(patch("/api/v1/categories/2/parameters")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"maxCoverageDaysThreshold\": 15, \"defaultExtraCoverageDays\": 5}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categoryId").value(2))
				.andExpect(jsonPath("$.maxCoverageDaysThreshold").value(15))
				.andExpect(jsonPath("$.defaultExtraCoverageDays").value(5));

		String persisted = Files.readString(tempCsvDir.resolve("categorias.csv"));
		assertTrue(persisted.lines().anyMatch(line -> line.equals("2,Lácteos,1,15,5")));
	}

	@Test
	void returns404ForAnUnknownCategory() throws Exception {
		mockMvc.perform(patch("/api/v1/categories/9999/parameters")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"maxCoverageDaysThreshold\": 15, \"defaultExtraCoverageDays\": 5}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsANonPositiveThreshold() throws Exception {
		mockMvc.perform(patch("/api/v1/categories/2/parameters")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"maxCoverageDaysThreshold\": 0, \"defaultExtraCoverageDays\": 5}"))
				.andExpect(status().isBadRequest());
	}
}

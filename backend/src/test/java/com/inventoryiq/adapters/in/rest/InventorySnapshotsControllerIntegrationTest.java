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
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * De punta a punta: HTTP -> InventorySnapshotsController ->
 * RecordInventoryCountUseCase (wiring real de config/) -> adaptadores CSV
 * reales -> un CSV aislado (copia de src/test/resources/csv-fixtures en un
 * @TempDir, nunca data/csv real — mismo criterio que
 * CsvIngestionControllerIntegrationTest: este slice también escribe).
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class InventorySnapshotsControllerIntegrationTest {

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
	void recordsAManualCountAndPersistsItThroughTheRealWiring() throws Exception {
		String today = LocalDate.now().toString();

		mockMvc.perform(post("/api/v1/inventory-snapshots")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productId\": 1001, \"storeId\": 1, \"stockActual\": 35}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(1001))
				.andExpect(jsonPath("$.storeId").value(1))
				.andExpect(jsonPath("$.currentStock").value(35))
				.andExpect(jsonPath("$.stockInTransit").value(0))
				.andExpect(jsonPath("$.snapshotDate").value(today));

		String persisted = Files.readString(tempCsvDir.resolve("inventario.csv"));
		assertTrue(persisted.lines().anyMatch(line -> line.equals("900006," + today + ",1001,1,35,0")));
	}

	@Test
	void returns404ForAnUnknownProduct() throws Exception {
		mockMvc.perform(post("/api/v1/inventory-snapshots")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productId\": 9999, \"storeId\": 1, \"stockActual\": 10}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsANegativeCount() throws Exception {
		mockMvc.perform(post("/api/v1/inventory-snapshots")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productId\": 1001, \"storeId\": 1, \"stockActual\": -1}"))
				.andExpect(status().isBadRequest());
	}
}

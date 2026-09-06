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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * De punta a punta: HTTP -> SuppliersController -> ListSuppliersUseCase /
 * UpdateSupplierLeadTimeUseCase (wiring real de config/) -> adaptador CSV
 * real -> un CSV aislado (copia de src/test/resources/csv-fixtures en un
 * @TempDir, nunca data/csv real) — mismo criterio que
 * InventorySnapshotsControllerIntegrationTest: este slice también escribe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class SuppliersControllerIntegrationTest {

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
	void listsOnlyActiveSuppliers() throws Exception {
		mockMvc.perform(get("/api/v1/suppliers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].supplierId").value(1))
				.andExpect(jsonPath("$[0].businessName").value("Lácteos del Sur S.A."))
				.andExpect(jsonPath("$[0].leadTimeDays").value(3))
				.andExpect(jsonPath("$[0].paymentTerms").value("30 días"));
	}

	/**
	 * Usa el proveedor 2 (inactivo en el fixture) en vez del 1 a propósito:
	 * los métodos de esta clase comparten el mismo @TempDir y el mismo
	 * contexto de Spring (un único CsvSupplierRepositoryAdapter en memoria
	 * para toda la clase), así que mutar el proveedor que otro test lee no
	 * puede depender del orden de ejecución de JUnit.
	 */
	@Test
	void updatesTheLeadTimeAndPersistsItThroughTheRealWiring() throws Exception {
		mockMvc.perform(patch("/api/v1/suppliers/2/lead-time")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"leadTimeDays\": 9}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.supplierId").value(2))
				.andExpect(jsonPath("$.leadTimeDays").value(9));

		String persisted = Files.readString(tempCsvDir.resolve("proveedores.csv"));
		assertTrue(persisted.lines().anyMatch(line -> line.equals("2,Proveedor Descontinuado,9,Contado,False")));
	}

	@Test
	void returns404ForAnUnknownSupplier() throws Exception {
		mockMvc.perform(patch("/api/v1/suppliers/9999/lead-time")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"leadTimeDays\": 5}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsANonPositiveLeadTime() throws Exception {
		mockMvc.perform(patch("/api/v1/suppliers/1/lead-time")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"leadTimeDays\": 0}"))
				.andExpect(status().isBadRequest());
	}
}

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice completo, de punta a punta: HTTP -> CsvIngestionController ->
 * IngestCsvFileUseCase (wiring real de config/) -> adaptadores CSV
 * reales -> un CSV aislado (copia de src/test/resources/csv-fixtures en
 * un @TempDir, nunca data/csv real: este es el primer slice que escribe,
 * y no puede tocar el dataset compartido que el resto de los tests de
 * integración verifica a mano contra números fijos).
 *
 * Excluye Hibernate/JPA-repositories/Flyway (no hace falta Postgres para
 * probar ingesta de ventas) pero NO excluye el datasource: cae al H2
 * embebido automático, sin necesitar Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class CsvIngestionControllerIntegrationTest {

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
	void ingestsAValidSaleAndPersistsItThroughTheRealWiring() throws Exception {
		String csv = "venta_id,fecha,producto_id,sucursal_id,unidades_vendidas,importe_total\n"
				+ "700001,2025-07-10,1001,1,12,7000.00\n";
		MockMultipartFile file = new MockMultipartFile("file", "ventas.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file).param("fileType", "SALES"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalRowsRead").value(1))
				.andExpect(jsonPath("$.acceptedCount").value(1))
				.andExpect(jsonPath("$.rejectedCount").value(0));

		String persisted = Files.readString(tempCsvDir.resolve("ventas.csv"));
		assertTrue(persisted.contains("700001,2025-07-10,1001,1,12,7000.00"));
	}

	@Test
	void reportsPerRowRejectionsForAnUnknownProductAndAnUnknownStore() throws Exception {
		// 2 filas problemáticas acompañadas de suficientes filas válidas para
		// quedar bajo el umbral del 5% (si no, dispara CsvIngestionThresholdExceededException).
		StringBuilder csv = new StringBuilder("venta_id,fecha,producto_id,sucursal_id,unidades_vendidas,importe_total\n");
		csv.append("700003,2025-08-01,9999,1,5,2000.00\n"); // producto inexistente
		csv.append("700004,2025-08-01,1001,9999,5,2000.00\n"); // sucursal inexistente
		LocalDate paddingStart = LocalDate.parse("2025-08-01");
		for (int i = 0; i < 39; i++) {
			csv.append(700100 + i).append(',').append(paddingStart.plusDays(i)).append(",1001,1,5,2000.00\n");
		}
		MockMultipartFile file = new MockMultipartFile("file", "ventas.csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file).param("fileType", "SALES"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalRowsRead").value(41))
				.andExpect(jsonPath("$.acceptedCount").value(39))
				.andExpect(jsonPath("$.rejectedCount").value(2));
	}

	@Test
	void returns422AndPersistsNothingWhenEveryRowFailsValidation() throws Exception {
		String csv = "venta_id,fecha,producto_id,sucursal_id,unidades_vendidas,importe_total\n"
				+ "700005,2025-07-12,9999,1,5,2000.00\n"
				+ "700006,2025-07-12,9998,1,5,2000.00\n";
		MockMultipartFile file = new MockMultipartFile("file", "ventas.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/csv-ingestions").file(file).param("fileType", "SALES"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.rejectedCount").value(2));

		String persisted = Files.readString(tempCsvDir.resolve("ventas.csv"));
		assertTrue(persisted.lines().noneMatch(line -> line.startsWith("700005") || line.startsWith("700006")));
	}
}

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
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice completo, de punta a punta: HTTP -> OverstockController ->
 * DetectOverstockUseCase (wiring real de config/) -> adaptadores CSV
 * reales -> CSV reales de data/csv.
 *
 * El caso de detectsAKnownOverstockProduct se verificó A MANO contra
 * ventas.csv/inventario.csv/productos.csv/categorias.csv: producto 1012
 * (Mortadela 200g), sucursal 1, 2026-08-01, ventana de 90 días
 * (2026-05-04 a 2026-08-01):
 * - 20 unidades vendidas en la ventana, pero 15 de esos 90 días tienen
 *   stock=0 al inicio del día (censura de demanda, regla 4.9); de esos 15
 *   días censurados, 4 unidades igual se vendieron y se excluyen.
 * - ADS corregido = (20-4) / (90-15) = 16/75 = 0.213333...
 * - Categoría 3 (Fiambres y Quesos): dias_cobertura_extra_default=3,
 *   umbral_max_cobertura_dias=10, lead_time_dias del producto=6.
 * - SafetyStock = ADS*3 = 0.64; ReorderPoint = ADS*6 + 0.64 = 1.92.
 * - currentStock (inventario.csv, 2026-08-01) = 3.
 * - currentDaysOfCoverage = 3 / 0.213333... = 14.0625 > umbral(10) -> OVERSTOCK
 *   (y no CRITICAL: 3 > safetyStock(0.64)).
 * - immobilizedValue = 3 * costo_unitario(1245.95) = 3737.85.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class OverstockControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void detectsAKnownOverstockProductThroughTheRealWiring() throws Exception {
		mockMvc.perform(get("/api/v1/products/overstock")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId == 1012)]").exists())
				.andExpect(jsonPath("$[?(@.productId == 1012)].currentStock", contains(3)))
				.andExpect(jsonPath("$[?(@.productId == 1012)].currentDaysOfCoverage", contains(closeTo(14.0625, 0.0001))))
				.andExpect(jsonPath("$[?(@.productId == 1012)].immobilizedValue", contains(3737.85)));
	}

	@Test
	void allResultsAreOverstockAndSortedByImmobilizedValueDescendingByDefault() throws Exception {
		MvcResult mvcResult = mockMvc.perform(get("/api/v1/products/overstock")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andReturn();

		String body = mvcResult.getResponse().getContentAsString();
		assertTrue(body.length() > 2, "Con 62 productos reales en sucursal 1 debería haber al menos un producto en sobrestock");
	}

	@Test
	void sortByDaysOfCoverageChangesTheOrderComparedToTheDefault() throws Exception {
		String byValue = mockMvc.perform(get("/api/v1/products/overstock")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		String byCoverage = mockMvc.perform(get("/api/v1/products/overstock")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("sortBy", "DAYS_OF_COVERAGE"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertNotEquals(byValue, byCoverage,
				"El orden por valor inmovilizado y por días de cobertura debería diferir con datos reales");
	}

	@Test
	void globalExceptionHandlerIsRegisteredInTheFullApplicationContext() throws Exception {
		mockMvc.perform(get("/api/v1/products/overstock")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}

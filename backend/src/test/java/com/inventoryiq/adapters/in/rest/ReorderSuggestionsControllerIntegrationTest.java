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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice completo, de punta a punta: HTTP -> ReorderSuggestionsController ->
 * GenerateReorderSuggestionsUseCase (wiring real de config/) -> adaptadores
 * CSV reales -> CSV reales de data/csv.
 *
 * El caso suggestsAKnownStockoutProduct se verificó A MANO con gawk contra
 * ventas.csv/inventario.csv/productos.csv: producto 1006 (Crema de Leche
 * 200ml), sucursal 2, 2026-02-01, ventana default de 90 días
 * (2025-11-04 a 2026-02-01):
 * - 0 de 90 días censurados en esta ventana -> ADS corregido = 224/90 = 2.48889...
 * - currentStock=0, stockInTransit=24 (inventario.csv, 2026-02-01).
 * - Cantidad sugerida = round(ADS×15 − stock actual − en tránsito)
 *   = round(2.48889×15 − 0 − 24) = round(13.333) = 13.
 * - Fecha límite: como currentStock(0) ya está por debajo del stock de
 *   seguridad (ADS×3 ≈ 7.47 > 0), la proyección da un cruce en el pasado,
 *   así que el límite queda en la propia fecha de referencia (hoy).
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class ReorderSuggestionsControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void suggestsAKnownStockoutProductThroughTheRealWiring() throws Exception {
		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("storeId", "2")
						.param("referenceDate", "2026-02-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId == 1006)]").exists())
				.andExpect(jsonPath("$[?(@.productId == 1006)].supplierId", contains(1)))
				.andExpect(jsonPath("$[?(@.productId == 1006)].suggestedQuantity", contains(13)))
				.andExpect(jsonPath("$[?(@.productId == 1006)].orderDeadlineDate", contains("2026-02-01")));
	}

	@Test
	void everySuggestionHasAPositiveQuantityAndNonBlankJustification() throws Exception {
		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", not(empty())))
				.andExpect(jsonPath("$[*].suggestedQuantity", everyItem(greaterThanOrEqualTo(0))))
				.andExpect(jsonPath("$[*].justification", everyItem(not(emptyString()))));
	}

	@Test
	void globalExceptionHandlerIsRegisteredInTheFullApplicationContext() throws Exception {
		mockMvc.perform(get("/api/v1/reorder-suggestions")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}

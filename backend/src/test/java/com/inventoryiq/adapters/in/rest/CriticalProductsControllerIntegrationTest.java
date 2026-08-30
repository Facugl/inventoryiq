package com.inventoryiq.adapters.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice 2 completo, de punta a punta: HTTP -> CriticalProductsController ->
 * GetCriticalProductsUseCase (wiring real de config/) -> adaptadores CSV
 * reales -> CSV reales de data/csv. Excluye JPA/Flyway/DataSource igual que
 * InventoryIqApplicationTests, por el mismo motivo (sin datasource local).
 *
 * Reutiliza el mismo caso verificado a mano en
 * GetCriticalProductsIntegrationTest (Slice 1): producto 1006 (Crema de
 * Leche 200ml), sucursal 2, 2026-02-01, stock_actual=0 -> CRITICAL
 * garantizado sin depender de ninguna fórmula.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class CriticalProductsControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsAKnownStockoutAsCriticalThroughTheRealWiring() throws Exception {
		mockMvc.perform(get("/api/v1/products/critical")
						.param("storeId", "2")
						.param("referenceDate", "2026-02-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId == 1006)]").exists())
				.andExpect(jsonPath("$[?(@.productId == 1006)].status").value(contains("CRITICAL")))
				.andExpect(jsonPath("$[?(@.productId == 1006)].currentStock").value(contains(0)));
	}

	@Test
	void globalExceptionHandlerIsRegisteredInTheFullApplicationContext() throws Exception {
		// GlobalExceptionHandler está probado de forma aislada en
		// CriticalProductsControllerTest (@WebMvcTest); acá se confirma que
		// también queda registrado cuando arranca el contexto completo de la
		// aplicación (component-scanning real), no solo en el slice de MVC.
		mockMvc.perform(get("/api/v1/products/critical")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}

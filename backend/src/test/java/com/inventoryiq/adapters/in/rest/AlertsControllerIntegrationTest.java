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
 * Slice completo, de punta a punta: HTTP -> AlertsController ->
 * GenerateAlertsUseCase (wiring real de config/) -> GetCriticalProductsUseCase
 * y DetectOverstockUseCase reales -> adaptadores CSV reales -> CSV reales.
 *
 * Reutiliza casos ya verificados a mano en slices anteriores (no hace
 * falta recalcular nada nuevo, la composición no agrega fórmulas propias):
 * - Producto 1006 (Crema de Leche 200ml), sucursal 2, 2026-02-01: stock=0
 *   (Slice GetCriticalProducts). Cualquier producto con stock=0 tiene
 *   coverageFactor=1 e indicador de quiebre=1 en la fórmula 4.11, así que
 *   su score nunca puede bajar de (1/3*0.3*100)+(1/3*100)+(1/3*100)=76.67
 *   incluso en el peor caso (clase C) -> siempre cae en HIGH
 *   (CriticalityLevel.isCritical(), umbral 75). No depende de a qué clase
 *   ABC pertenezca este producto en particular.
 * - Producto 1012 (Mortadela 200g), sucursal 1, 2026-08-01:
 *   immobilizedValue=3737.85 (Slice DetectOverstock) -> por debajo de
 *   $50.000 -> LOW.
 * - Producto 1061 (Milanesa de Nalga), sucursal 1, 2026-08-01:
 *   immobilizedValue=1.846.434,06 (Slice DetectOverstock) -> por encima
 *   de $500.000 -> HIGH.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		HibernateJpaAutoConfiguration.class,
		DataJpaRepositoriesAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class AlertsControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void aKnownStockoutProductAlwaysProducesAHighSeverityStockoutAlert() throws Exception {
		mockMvc.perform(get("/api/v1/alerts")
						.param("storeId", "2")
						.param("referenceDate", "2026-02-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId == 1006)]").exists())
				.andExpect(jsonPath("$[?(@.productId == 1006)].type", contains("STOCKOUT")))
				.andExpect(jsonPath("$[?(@.productId == 1006)].severity", contains("HIGH")))
				.andExpect(jsonPath("$[?(@.productId == 1006)].generatedAt", contains("2026-02-01")));
	}

	@Test
	void knownOverstockProductsGetSeverityFromImmobilizedValueThresholds() throws Exception {
		mockMvc.perform(get("/api/v1/alerts")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("type", "OVERSTOCK"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId == 1012)].severity", contains("LOW")))
				.andExpect(jsonPath("$[?(@.productId == 1061)].severity", contains("HIGH")));
	}

	@Test
	void typeFilterOnlyReturnsTheRequestedAlertType() throws Exception {
		mockMvc.perform(get("/api/v1/alerts")
						.param("storeId", "1")
						.param("referenceDate", "2026-08-01")
						.param("type", "OVERSTOCK"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.type == 'STOCKOUT')]").doesNotExist());
	}

	@Test
	void globalExceptionHandlerIsRegisteredInTheFullApplicationContext() throws Exception {
		mockMvc.perform(get("/api/v1/alerts")
						.param("referenceDate", "2026-08-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}

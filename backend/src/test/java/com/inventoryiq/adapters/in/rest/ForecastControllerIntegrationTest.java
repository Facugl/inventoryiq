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

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice completo, de punta a punta: HTTP -> ForecastController ->
 * ForecastDemandUseCase (wiring real de config/) -> adaptadores CSV
 * reales -> CSV reales de data/csv.
 *
 * El caso projectsAKnownProductThroughTheRealWiring se verificó A MANO
 * con gawk contra ventas.csv/inventario.csv: producto 1001, sucursal 1,
 * referenceDate=2026-07-24, ventana de 365 días
 * (2025-07-25 a 2026-07-24):
 * - 364 de 365 días válidos (sin quiebre) en la ventana -> ADS base
 *   corregido = 15733/364 = 43.222527...
 * - El primer período proyectado (horizonDays=7) es 2026-07-25 a
 *   2026-07-31, dentro de julio de 2026: ese mes ya tiene 24 días de
 *   historial propio dentro de la ventana (1 al 24 de julio de 2026),
 *   con ADS de julio = 954/24 = 39.75.
 * - Índice estacional = 39.75 / 43.222527 = 0.919659...
 * - ADS proyectado del período = ADS base × índice = 39.75 (coincide con
 *   el ADS histórico real de julio, por construcción).
 * - Demanda proyectada del período = round(39.75 × 7) = 278.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
class ForecastControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void projectsAKnownProductThroughTheRealWiring() throws Exception {
		mockMvc.perform(get("/api/v1/products/1001/forecast")
						.param("storeId", "1")
						.param("referenceDate", "2026-07-24")
						.param("horizonDays", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(1001))
				.andExpect(jsonPath("$.baseAds").value(closeTo(43.222527, 1e-4)))
				.andExpect(jsonPath("$.periods.length()").value(1))
				.andExpect(jsonPath("$.periods[0].periodStart").value("2026-07-25"))
				.andExpect(jsonPath("$.periods[0].periodEnd").value("2026-07-31"))
				.andExpect(jsonPath("$.periods[0].seasonalIndex").value(closeTo(0.919659, 1e-4)))
				.andExpect(jsonPath("$.periods[0].projectedDailyAds").value(closeTo(39.75, 1e-4)))
				.andExpect(jsonPath("$.periods[0].projectedTotalDemand").value(278));
	}

	@Test
	void aLongerHorizonIsSplitIntoWeeklyPeriods() throws Exception {
		mockMvc.perform(get("/api/v1/products/1001/forecast")
						.param("storeId", "1")
						.param("referenceDate", "2026-07-24")
						.param("horizonDays", "14"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods.length()").value(2))
				.andExpect(jsonPath("$.periods[1].periodStart").value("2026-08-01"))
				.andExpect(jsonPath("$.periods[1].periodEnd").value("2026-08-07"));
	}

	@Test
	void returns404WhenTheProductDoesNotExistThroughTheRealWiring() throws Exception {
		mockMvc.perform(get("/api/v1/products/999999/forecast")
						.param("storeId", "1")
						.param("referenceDate", "2026-07-24")
						.param("horizonDays", "7"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void globalExceptionHandlerIsRegisteredInTheFullApplicationContext() throws Exception {
		mockMvc.perform(get("/api/v1/products/1001/forecast")
						.param("referenceDate", "2026-07-24")
						.param("horizonDays", "7"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}

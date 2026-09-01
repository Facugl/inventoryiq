package com.inventoryiq.adapters.in.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice completo, de punta a punta: HTTP -> KPIsController ->
 * CalculateInventoryKPIsUseCase (wiring real de config/) -> adaptadores
 * CSV reales + adaptador Postgres real (Testcontainers, tabla
 * recommendations vacía) -> data/csv real. Necesita Docker Desktop
 * corriendo.
 *
 * Los tres primeros valores se verificaron a mano con gawk contra
 * productos.csv/ventas.csv/inventario.csv: sucursal 2, toDate=2026-02-01,
 * ventana de 90 días (2025-11-04 a 2026-02-01):
 * - 58 productos activos con historial suficiente, 2 sin stock -> tasa de
 *   quiebre = 2/58 = 3.448276%.
 * - cobertura promedio = 10.230573 días.
 * - rotación (fromDate=2026-01-01, toDate=2026-02-01): COGS=44722646.64,
 *   inventario promedio=15002105.1747 -> rotación=2.981091.
 * El capital inmovilizado en sobrestock se verifica por consistencia
 * contra GET /api/v1/products/overstock (mismo store/fecha): tiene que
 * coincidir con la suma de immobilizedValue de esa respuesta, ya
 * verificada a mano en OverstockControllerIntegrationTest.
 * % de recomendaciones seguidas da null: la tabla recommendations está
 * vacía (Postgres efímero de Testcontainers, sin ningún recalculate ni
 * feedback ejecutado en este test).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class KPIsControllerIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void computesKPIsForAKnownStoreAndPeriodThroughTheRealWiring() throws Exception {
		mockMvc.perform(get("/api/v1/kpis")
						.param("storeId", "2")
						.param("fromDate", "2026-01-01")
						.param("toDate", "2026-02-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stockoutRate").value(closeTo(3.448276, 1e-4)))
				.andExpect(jsonPath("$.averageDaysOfCoverage").value(closeTo(10.230573, 1e-4)))
				.andExpect(jsonPath("$.inventoryTurnover").value(closeTo(2.981091, 1e-4)))
				.andExpect(jsonPath("$.recommendationsFollowedRate").doesNotExist());
	}

	@Test
	void immobilizedOverstockValueMatchesTheOverstockEndpointForTheSameScope() throws Exception {
		String overstockJson = mockMvc.perform(get("/api/v1/products/overstock")
						.param("storeId", "2")
						.param("referenceDate", "2026-02-01"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		List<Number> values = JsonPath.read(overstockJson, "$[*].immobilizedValue");
		double expectedTotal = values.stream().mapToDouble(Number::doubleValue).sum();

		mockMvc.perform(get("/api/v1/kpis")
						.param("storeId", "2")
						.param("fromDate", "2026-01-01")
						.param("toDate", "2026-02-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.immobilizedOverstockValue").value(closeTo(expectedTotal, 1e-2)));
	}

	@Test
	void globalExceptionHandlerIsRegisteredInTheFullApplicationContext() throws Exception {
		mockMvc.perform(get("/api/v1/kpis")
						.param("fromDate", "2026-01-01")
						.param("toDate", "2026-02-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}

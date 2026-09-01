package com.inventoryiq.adapters.in.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice completo, de punta a punta: HTTP -> ProductStatusController ->
 * RecalculateProductStatusUseCase (wiring real de config/) -> los 4
 * casos de uso que encadena -> adaptadores CSV reales + Postgres real
 * (Testcontainers). Necesita Docker Desktop corriendo.
 *
 * El caso para la sucursal 2 reutiliza el mismo escenario ya verificado
 * a mano en RecommendationsControllerIntegrationTest (Slice 8):
 * recalcular la sucursal 2 al 2026-02-01, con la tabla recommendations
 * vacía, genera 23 recomendaciones nuevas. Acá se verifica además por
 * consistencia cruzada contra GET /api/v1/products/critical,
 * GET /api/v1/products/overstock y GET /api/v1/alerts para la misma
 * sucursal/fecha — ambos caminos tienen que coincidir, porque
 * RecalculateProductStatusService llama exactamente a esos mismos casos
 * de uso.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(ProductStatusControllerIntegrationTest.FixedClockConfig.class)
class ProductStatusControllerIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

	@TestConfiguration
	static class FixedClockConfig {
		@Bean
		@Primary
		Clock testClock() {
			return Clock.fixed(Instant.parse("2026-02-01T00:00:00Z"), ZoneOffset.UTC);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM recommendations");
	}

	@Test
	void recalculatesAKnownStoreAndMatchesTheUnderlyingEndpointsThroughTheRealWiring() throws Exception {
		String criticalJson = mockMvc.perform(get("/api/v1/products/critical").param("storeId", "2").param("referenceDate", "2026-02-01"))
				.andReturn().getResponse().getContentAsString();
		String overstockJson = mockMvc.perform(get("/api/v1/products/overstock").param("storeId", "2").param("referenceDate", "2026-02-01"))
				.andReturn().getResponse().getContentAsString();
		String alertsJson = mockMvc.perform(get("/api/v1/alerts").param("storeId", "2").param("referenceDate", "2026-02-01"))
				.andReturn().getResponse().getContentAsString();

		int expectedCritical = JsonPath.<Integer>read(criticalJson, "$.length()");
		int expectedOverstock = JsonPath.<Integer>read(overstockJson, "$.length()");
		int expectedAlerts = JsonPath.<Integer>read(alertsJson, "$.length()");

		mockMvc.perform(post("/api/v1/product-status/recalculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"storeId\": 2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.storesProcessed").value(1))
				.andExpect(jsonPath("$.perStore[0].storeId").value(2))
				.andExpect(jsonPath("$.perStore[0].criticalProductsFound").value(expectedCritical))
				.andExpect(jsonPath("$.perStore[0].overstockProductsFound").value(expectedOverstock))
				.andExpect(jsonPath("$.perStore[0].alertsGenerated").value(expectedAlerts))
				.andExpect(jsonPath("$.perStore[0].recommendations.newCount").value(23))
				.andExpect(jsonPath("$.perStore[0].recommendations.updatedCount").value(0));
	}

	@Test
	void recalculatesEveryActiveStoreWhenTheRequestBodyIsOmitted() throws Exception {
		mockMvc.perform(post("/api/v1/product-status/recalculate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.storesProcessed").value(3))
				.andExpect(jsonPath("$.perStore[0].storeId").value(1))
				.andExpect(jsonPath("$.perStore[1].storeId").value(2))
				.andExpect(jsonPath("$.perStore[2].storeId").value(3));
	}

	@Test
	void globalExceptionHandlerIsRegisteredInTheFullApplicationContext() throws Exception {
		mockMvc.perform(post("/api/v1/product-status/recalculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"storeId\": 0}"))
				.andExpect(status().isBadRequest());
	}
}

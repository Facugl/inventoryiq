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
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice completo, de punta a punta: HTTP -> RecommendationsController ->
 * los tres casos de uso (wiring real de config/) -> adaptador Postgres
 * real (Testcontainers, con la migración V1 real aplicada por Flyway) +
 * adaptadores CSV reales -> CSV reales de data/csv. Necesita Docker
 * Desktop corriendo (arranca un Postgres real efímero).
 *
 * El escenario se reutiliza del ya verificado a mano en
 * ReorderSuggestionsControllerIntegrationTest: producto 1006 (Crema de
 * Leche 200ml), sucursal 2, 2026-02-01 -> proveedor 1, cantidad sugerida
 * 13, fecha límite 2026-02-01 (ver el Javadoc de esa clase para el
 * detalle del cálculo a mano con gawk). RecalculateRecommendationsService
 * llama al mismo GenerateReorderSuggestionsUseCase con los mismos
 * parámetros, así que los números son los mismos — lo que este test
 * verifica es la persistencia y el ciclo de vida (alta, feedback,
 * no-duplicación en una segunda corrida), no el cálculo en sí.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecommendationsControllerIntegrationTest.FixedClockConfig.class)
class RecommendationsControllerIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

	@TestConfiguration
	static class FixedClockConfig {
		// Nombre de bean distinto de "clock" (el de ClockConfig, ya en el contexto completo de
		// @SpringBootTest) + @Primary: evita el choque de nombres, y sigue ganando por tipo.
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
	void generatesListsAppliesFeedbackAndDoesNotDuplicateOnANewRunThroughTheRealWiring() throws Exception {
		mockMvc.perform(post("/api/v1/recommendations/recalculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"storeId\": 2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.newCount").value(greaterThan(0)))
				.andExpect(jsonPath("$.updatedCount").value(0));

		String pendingListJson = mockMvc.perform(get("/api/v1/recommendations")
						.param("storeId", "2")
						.param("status", "PENDING"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId == 1006)]").exists())
				.andExpect(jsonPath("$[?(@.productId == 1006)].supplierId", contains(1)))
				.andExpect(jsonPath("$[?(@.productId == 1006)].suggestedQuantity", contains(13)))
				.andExpect(jsonPath("$[?(@.productId == 1006)].orderDeadlineDate", contains("2026-02-01")))
				.andReturn().getResponse().getContentAsString();

		List<Number> matchingIds = JsonPath.read(pendingListJson, "$[?(@.productId == 1006)].recommendationId");
		long recommendationId = matchingIds.get(0).longValue();

		mockMvc.perform(patch("/api/v1/recommendations/" + recommendationId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\": \"APPLIED\", \"comment\": \"comprado a proveedor 1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPLIED"))
				.andExpect(jsonPath("$.feedbackComment").value("comprado a proveedor 1"));

		// segunda corrida el mismo día: la recomendación ya resuelta no se toca ni se duplica.
		mockMvc.perform(post("/api/v1/recommendations/recalculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"storeId\": 2}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/recommendations")
						.param("storeId", "2")
						.param("status", "PENDING"))
				.andExpect(jsonPath("$[?(@.productId == 1006)]").doesNotExist());

		mockMvc.perform(get("/api/v1/recommendations")
						.param("storeId", "2")
						.param("status", "APPLIED"))
				.andExpect(jsonPath("$[?(@.productId == 1006)]").exists());
	}

	@Test
	void patchReturns404WhenTheRecommendationDoesNotExistThroughTheRealWiring() throws Exception {
		mockMvc.perform(patch("/api/v1/recommendations/999999")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\": \"APPLIED\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void globalExceptionHandlerIsRegisteredInTheFullApplicationContext() throws Exception {
		mockMvc.perform(get("/api/v1/recommendations"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}

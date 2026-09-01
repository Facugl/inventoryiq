package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.application.port.in.ListRecommendationsQuery;
import com.inventoryiq.application.port.in.ListRecommendationsUseCase;
import com.inventoryiq.application.port.in.RecalculateRecommendationsCommand;
import com.inventoryiq.application.port.in.RecalculateRecommendationsResult;
import com.inventoryiq.application.port.in.RecalculateRecommendationsUseCase;
import com.inventoryiq.application.port.in.RecommendationResult;
import com.inventoryiq.application.port.in.RegisterRecommendationFeedbackCommand;
import com.inventoryiq.application.port.in.RegisterRecommendationFeedbackUseCase;
import com.inventoryiq.domain.exception.RecommendationNotFoundException;
import com.inventoryiq.domain.model.RecommendationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el controller de forma aislada: los tres puertos de entrada están
 * mockeados, no hay Postgres ni CSV de por medio.
 */
@WebMvcTest(RecommendationsController.class)
@Import(RecommendationsControllerTest.FixedClockConfig.class)
class RecommendationsControllerTest {

	@TestConfiguration
	static class FixedClockConfig {
		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListRecommendationsUseCase listRecommendationsUseCase;

	@MockitoBean
	private RecalculateRecommendationsUseCase recalculateRecommendationsUseCase;

	@MockitoBean
	private RegisterRecommendationFeedbackUseCase registerRecommendationFeedbackUseCase;

	@Test
	void getReturnsRecommendationsAsJson() throws Exception {
		RecommendationResult result = new RecommendationResult(1L, 1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L, 5L,
				100, LocalDate.parse("2026-08-05"), "justificación", RecommendationStatus.PENDING,
				LocalDate.parse("2026-08-01"), null, null);
		given(listRecommendationsUseCase.execute(any(ListRecommendationsQuery.class))).willReturn(List.of(result));

		mockMvc.perform(get("/api/v1/recommendations").param("storeId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].recommendationId").value(1))
				.andExpect(jsonPath("$[0].status").value("PENDING"));
	}

	@Test
	void getReturns400WhenStoreIdIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/recommendations"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void postRecalculateReturnsTheExecutionSummary() throws Exception {
		given(recalculateRecommendationsUseCase.execute(any(RecalculateRecommendationsCommand.class)))
				.willReturn(new RecalculateRecommendationsResult(3, 2, 1, 1));

		mockMvc.perform(post("/api/v1/recommendations/recalculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"storeId\": 1}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalGenerated").value(3))
				.andExpect(jsonPath("$.newCount").value(2))
				.andExpect(jsonPath("$.updatedCount").value(1))
				.andExpect(jsonPath("$.autoDiscardedCount").value(1));
	}

	@Test
	void postRecalculateReturns400WhenStoreIdIsMissing() throws Exception {
		mockMvc.perform(post("/api/v1/recommendations/recalculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void patchRegistersFeedbackAndReturnsTheUpdatedRecommendation() throws Exception {
		RecommendationResult result = new RecommendationResult(1L, 1001L, "LEC-1001", "Leche Entera 1L", 1L, 2L, 5L,
				100, LocalDate.parse("2026-08-05"), "justificación", RecommendationStatus.APPLIED,
				LocalDate.parse("2026-08-01"), "comprado", LocalDate.parse("2026-08-03"));
		given(registerRecommendationFeedbackUseCase.execute(any(RegisterRecommendationFeedbackCommand.class)))
				.willReturn(result);

		mockMvc.perform(patch("/api/v1/recommendations/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\": \"APPLIED\", \"comment\": \"comprado\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPLIED"))
				.andExpect(jsonPath("$.feedbackComment").value("comprado"));
	}

	@Test
	void patchReturns400WhenStatusIsMissing() throws Exception {
		mockMvc.perform(patch("/api/v1/recommendations/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void patchReturns404WhenTheRecommendationDoesNotExist() throws Exception {
		given(registerRecommendationFeedbackUseCase.execute(any(RegisterRecommendationFeedbackCommand.class)))
				.willThrow(new RecommendationNotFoundException(999L));

		mockMvc.perform(patch("/api/v1/recommendations/999")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\": \"APPLIED\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}
}

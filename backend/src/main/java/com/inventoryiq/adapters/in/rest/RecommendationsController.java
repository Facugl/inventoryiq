package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.RecalculateRecommendationsRequest;
import com.inventoryiq.adapters.in.rest.dto.RecalculateRecommendationsResponse;
import com.inventoryiq.adapters.in.rest.dto.RecommendationResponse;
import com.inventoryiq.adapters.in.rest.dto.RegisterRecommendationFeedbackRequest;
import com.inventoryiq.adapters.in.rest.mapper.RecommendationResponseMapper;
import com.inventoryiq.application.port.in.ListRecommendationsQuery;
import com.inventoryiq.application.port.in.ListRecommendationsUseCase;
import com.inventoryiq.application.port.in.RecalculateRecommendationsCommand;
import com.inventoryiq.application.port.in.RecalculateRecommendationsResult;
import com.inventoryiq.application.port.in.RecalculateRecommendationsUseCase;
import com.inventoryiq.application.port.in.RegisterRecommendationFeedbackCommand;
import com.inventoryiq.application.port.in.RegisterRecommendationFeedbackUseCase;
import com.inventoryiq.domain.model.RecommendationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Adaptador de entrada REST para las Secciones 8.5 (listar), 8.6
 * (recalcular) y 8.7/9.8 (feedback) — las tres comparten la tabla
 * recommendations, así que se agrupan en un solo controller. Depende
 * únicamente de los tres puertos de entrada, nunca de los Services ni del
 * adaptador Postgres — el wiring concreto vive en config/UseCaseConfig.
 *
 * referenceDate/feedbackDate no son parámetros de request en ninguno de
 * los tres endpoints documentados: este controller es el único punto del
 * proyecto que lee la fecha del sistema, tal como documentan los Javadoc
 * de RecalculateRecommendationsCommand y RegisterRecommendationFeedbackCommand.
 * Se inyecta como Clock (en vez de llamar LocalDate.now() directo) para
 * poder fijar una fecha determinística en los tests de integración sin
 * tocar el reloj real del sistema.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@Validated
public class RecommendationsController {

	private final ListRecommendationsUseCase listRecommendationsUseCase;
	private final RecalculateRecommendationsUseCase recalculateRecommendationsUseCase;
	private final RegisterRecommendationFeedbackUseCase registerRecommendationFeedbackUseCase;
	private final Clock clock;

	public RecommendationsController(
			ListRecommendationsUseCase listRecommendationsUseCase,
			RecalculateRecommendationsUseCase recalculateRecommendationsUseCase,
			RegisterRecommendationFeedbackUseCase registerRecommendationFeedbackUseCase,
			Clock clock) {
		this.listRecommendationsUseCase = listRecommendationsUseCase;
		this.recalculateRecommendationsUseCase = recalculateRecommendationsUseCase;
		this.registerRecommendationFeedbackUseCase = registerRecommendationFeedbackUseCase;
		this.clock = clock;
	}

	@GetMapping
	public List<RecommendationResponse> getRecommendations(
			@RequestParam @NotNull @Positive Long storeId,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(required = false) @Positive Long supplierId,
			@RequestParam(required = false) RecommendationStatus status) {

		ListRecommendationsQuery query = new ListRecommendationsQuery(storeId, categoryId, supplierId, status);

		return listRecommendationsUseCase.execute(query).stream()
				.map(RecommendationResponseMapper::toResponse)
				.toList();
	}

	@PostMapping("/recalculate")
	public RecalculateRecommendationsResponse recalculate(@RequestBody @Valid RecalculateRecommendationsRequest request) {
		RecalculateRecommendationsCommand command = new RecalculateRecommendationsCommand(request.storeId(), LocalDate.now(clock));

		RecalculateRecommendationsResult result = recalculateRecommendationsUseCase.execute(command);

		return new RecalculateRecommendationsResponse(
				result.totalGenerated(), result.newCount(), result.updatedCount(), result.autoDiscardedCount());
	}

	@PatchMapping("/{recommendationId}")
	public RecommendationResponse registerFeedback(
			@PathVariable @Positive Long recommendationId,
			@RequestBody @Valid RegisterRecommendationFeedbackRequest request) {

		RegisterRecommendationFeedbackCommand command = new RegisterRecommendationFeedbackCommand(
				recommendationId, request.status(), request.comment(), LocalDate.now(clock));

		return RecommendationResponseMapper.toResponse(registerRecommendationFeedbackUseCase.execute(command));
	}
}

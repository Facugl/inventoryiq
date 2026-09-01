package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.GenerateReorderSuggestionsQuery;
import com.inventoryiq.application.port.in.GenerateReorderSuggestionsUseCase;
import com.inventoryiq.application.port.in.RecalculateRecommendationsCommand;
import com.inventoryiq.application.port.in.RecalculateRecommendationsResult;
import com.inventoryiq.application.port.in.RecalculateRecommendationsUseCase;
import com.inventoryiq.application.port.in.ReorderSuggestionResult;
import com.inventoryiq.application.port.out.RecommendationRepository;
import com.inventoryiq.domain.model.Recommendation;
import com.inventoryiq.domain.model.RecommendationStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementación de RecalculateRecommendationsUseCase (Sección 8.6).
 *
 * Reutiliza GenerateReorderSuggestionsUseCase (Sección 9.2) como motor de
 * cálculo — esta clase solo agrega el upsert contra RecommendationRepository:
 * 1. Corre el cálculo fresco (sin persistir) para la sucursal.
 * 2. Para cada sugerencia: si ya existe una recomendación PENDING para ese
 *    producto, la actualiza (cantidad/fecha límite/justificación
 *    recalculadas) — "actualizada". Si no existe PENDING pero sí una ya
 *    resuelta (APPLIED/DISCARDED) el mismo referenceDate, se la deja en
 *    paz — el comprador ya se expidió hoy sobre este producto, no se le
 *    genera una recomendación duplicada el mismo día solo porque el
 *    stock todavía no refleja su compra. Si no existe ninguna
 *    recomendación de hoy, inserta una nueva ("nueva").
 * 3. Toda recomendación que estaba PENDING pero cuyo producto ya no
 *    aparece en el cálculo fresco (ya no requiere reposición) se marca
 *    DISCARDED automáticamente — "descartada por resolución" (Sección
 *    8.6).
 */
public class RecalculateRecommendationsService implements RecalculateRecommendationsUseCase {

	private static final String AUTO_DISCARD_COMMENT =
			"Descartada automáticamente: el producto ya no requiere reposición al momento del recálculo.";

	private final GenerateReorderSuggestionsUseCase generateReorderSuggestionsUseCase;
	private final RecommendationRepository recommendationRepository;

	public RecalculateRecommendationsService(
			GenerateReorderSuggestionsUseCase generateReorderSuggestionsUseCase, RecommendationRepository recommendationRepository) {
		this.generateReorderSuggestionsUseCase = generateReorderSuggestionsUseCase;
		this.recommendationRepository = recommendationRepository;
	}

	@Override
	public RecalculateRecommendationsResult execute(RecalculateRecommendationsCommand command) {
		List<ReorderSuggestionResult> freshSuggestions = generateReorderSuggestionsUseCase.execute(
				new GenerateReorderSuggestionsQuery(command.storeId(), null, null, command.referenceDate()));

		List<Recommendation> existingForStore = recommendationRepository.findByFilters(command.storeId(), null, null);

		Map<Long, Recommendation> pendingByProductId = new HashMap<>();
		Set<Long> resolvedTodayProductIds = new HashSet<>();
		for (Recommendation r : existingForStore) {
			if (r.status() == RecommendationStatus.PENDING) {
				pendingByProductId.put(r.productId(), r);
			} else if (r.generationDate().equals(command.referenceDate())) {
				resolvedTodayProductIds.add(r.productId());
			}
		}

		int newCount = 0;
		int updatedCount = 0;
		Set<Long> productsStillNeeded = new HashSet<>();

		for (ReorderSuggestionResult suggestion : freshSuggestions) {
			productsStillNeeded.add(suggestion.productId());
			Recommendation existingPending = pendingByProductId.get(suggestion.productId());

			if (existingPending == null && resolvedTodayProductIds.contains(suggestion.productId())) {
				continue; // ya resuelta hoy: no se genera una recomendación duplicada el mismo día
			}

			Recommendation toSave = new Recommendation(
					existingPending != null ? existingPending.recommendationId() : null,
					suggestion.productId(), suggestion.storeId(), suggestion.supplierId(),
					suggestion.suggestedQuantity(), suggestion.orderDeadlineDate(), suggestion.justification(),
					RecommendationStatus.PENDING, command.referenceDate(), null, null);
			recommendationRepository.save(toSave);

			if (existingPending != null) {
				updatedCount++;
			} else {
				newCount++;
			}
		}

		List<Recommendation> currentlyPending = List.copyOf(pendingByProductId.values());

		int autoDiscardedCount = 0;
		for (Recommendation pending : currentlyPending) {
			if (!productsStillNeeded.contains(pending.productId())) {
				recommendationRepository.save(
						pending.withFeedback(RecommendationStatus.DISCARDED, AUTO_DISCARD_COMMENT, command.referenceDate()));
				autoDiscardedCount++;
			}
		}

		return new RecalculateRecommendationsResult(newCount + updatedCount, newCount, updatedCount, autoDiscardedCount);
	}
}

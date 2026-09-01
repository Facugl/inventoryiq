package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.GenerateReorderSuggestionsQuery;
import com.inventoryiq.application.port.in.GenerateReorderSuggestionsUseCase;
import com.inventoryiq.application.port.in.RecalculateRecommendationsCommand;
import com.inventoryiq.application.port.in.RecalculateRecommendationsResult;
import com.inventoryiq.application.port.in.ReorderSuggestionResult;
import com.inventoryiq.application.port.out.RecommendationRepository;
import com.inventoryiq.domain.model.Recommendation;
import com.inventoryiq.domain.model.RecommendationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecalculateRecommendationsServiceTest {

	private static final Long STORE_ID = 1L;
	private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-08-10");

	@Test
	void insertsANewRecommendationWhenNoneIsPendingForThatProduct() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		FakeGenerateReorderSuggestionsUseCase suggestions = new FakeGenerateReorderSuggestionsUseCase(
				List.of(suggestion(2001L, 100)));

		var service = new RecalculateRecommendationsService(suggestions, recommendations);

		RecalculateRecommendationsResult result = service.execute(new RecalculateRecommendationsCommand(STORE_ID, REFERENCE_DATE));

		assertEquals(1, result.newCount());
		assertEquals(0, result.updatedCount());
		assertEquals(0, result.autoDiscardedCount());
		assertEquals(1, recommendations.all().size());
		Recommendation saved = recommendations.all().get(0);
		assertNotNull(saved.recommendationId());
		assertEquals(RecommendationStatus.PENDING, saved.status());
		assertEquals(100, saved.suggestedQuantity());
	}

	@Test
	void updatesTheExistingPendingRecommendationForTheSameProductInsteadOfDuplicating() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		Recommendation existing = new Recommendation(1L, 2001L, STORE_ID, 5L, 50, LocalDate.parse("2026-08-05"),
				"vieja justificación", RecommendationStatus.PENDING, LocalDate.parse("2026-08-01"), null, null);
		recommendations.add(existing);
		FakeGenerateReorderSuggestionsUseCase suggestions = new FakeGenerateReorderSuggestionsUseCase(
				List.of(suggestion(2001L, 120)));

		var service = new RecalculateRecommendationsService(suggestions, recommendations);

		RecalculateRecommendationsResult result = service.execute(new RecalculateRecommendationsCommand(STORE_ID, REFERENCE_DATE));

		assertEquals(0, result.newCount());
		assertEquals(1, result.updatedCount());
		assertEquals(1, recommendations.all().size());
		Recommendation updated = recommendations.all().get(0);
		assertEquals(1L, updated.recommendationId()); // mismo id: se actualizó, no se duplicó
		assertEquals(120, updated.suggestedQuantity());
	}

	@Test
	void autoDiscardsAPendingRecommendationWhoseProductNoLongerNeedsReplenishment() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		Recommendation existing = new Recommendation(1L, 2001L, STORE_ID, 5L, 50, LocalDate.parse("2026-08-05"),
				"vieja justificación", RecommendationStatus.PENDING, LocalDate.parse("2026-08-01"), null, null);
		recommendations.add(existing);
		FakeGenerateReorderSuggestionsUseCase suggestions = new FakeGenerateReorderSuggestionsUseCase(List.of());

		var service = new RecalculateRecommendationsService(suggestions, recommendations);

		RecalculateRecommendationsResult result = service.execute(new RecalculateRecommendationsCommand(STORE_ID, REFERENCE_DATE));

		assertEquals(1, result.autoDiscardedCount());
		Recommendation discarded = recommendations.findById(1L).orElseThrow();
		assertEquals(RecommendationStatus.DISCARDED, discarded.status());
		assertTrue(discarded.feedbackComment().contains("ya no requiere reposición"));
	}

	@Test
	void neverTouchesARecommendationAlreadyResolvedByTheUser() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		Recommendation applied = new Recommendation(1L, 2001L, STORE_ID, 5L, 50, LocalDate.parse("2026-08-05"),
				"justificación", RecommendationStatus.APPLIED, LocalDate.parse("2026-08-01"), "comprado", LocalDate.parse("2026-08-02"));
		recommendations.add(applied);
		FakeGenerateReorderSuggestionsUseCase suggestions = new FakeGenerateReorderSuggestionsUseCase(List.of());

		var service = new RecalculateRecommendationsService(suggestions, recommendations);

		service.execute(new RecalculateRecommendationsCommand(STORE_ID, REFERENCE_DATE));

		assertEquals(RecommendationStatus.APPLIED, recommendations.findById(1L).orElseThrow().status());
	}

	@Test
	void doesNotDuplicateARecommendationAlreadyResolvedTodayForTheSameProduct() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		Recommendation appliedToday = new Recommendation(1L, 2001L, STORE_ID, 5L, 50, LocalDate.parse("2026-08-05"),
				"justificación", RecommendationStatus.APPLIED, REFERENCE_DATE, "comprado", REFERENCE_DATE);
		recommendations.add(appliedToday);
		// El stock todavía no refleja la compra: el cálculo fresco sigue sugiriendo el mismo producto.
		FakeGenerateReorderSuggestionsUseCase suggestions = new FakeGenerateReorderSuggestionsUseCase(
				List.of(suggestion(2001L, 100)));

		var service = new RecalculateRecommendationsService(suggestions, recommendations);

		RecalculateRecommendationsResult result = service.execute(new RecalculateRecommendationsCommand(STORE_ID, REFERENCE_DATE));

		assertEquals(0, result.newCount());
		assertEquals(0, result.updatedCount());
		assertEquals(1, recommendations.all().size()); // no se duplicó
		assertEquals(RecommendationStatus.APPLIED, recommendations.findById(1L).orElseThrow().status());
	}

	// ---- helpers ----

	private static ReorderSuggestionResult suggestion(Long productId, int suggestedQuantity) {
		return new ReorderSuggestionResult(productId, "SKU-" + productId, "Producto " + productId, STORE_ID, 10L, 5L,
				suggestedQuantity, REFERENCE_DATE.plusDays(2), "justificación recalculada");
	}

	private static class FakeGenerateReorderSuggestionsUseCase implements GenerateReorderSuggestionsUseCase {
		private final List<ReorderSuggestionResult> results;

		FakeGenerateReorderSuggestionsUseCase(List<ReorderSuggestionResult> results) {
			this.results = results;
		}

		@Override
		public List<ReorderSuggestionResult> execute(GenerateReorderSuggestionsQuery query) {
			return results;
		}
	}

	private static class FakeRecommendationRepository implements RecommendationRepository {
		private final Map<Long, Recommendation> recommendations = new HashMap<>();
		private long nextId = 1L;

		void add(Recommendation recommendation) {
			recommendations.put(recommendation.recommendationId(), recommendation);
			nextId = Math.max(nextId, recommendation.recommendationId() + 1);
		}

		List<Recommendation> all() {
			return List.copyOf(recommendations.values());
		}

		@Override
		public Recommendation save(Recommendation recommendation) {
			Recommendation toStore = recommendation.recommendationId() == null
					? new Recommendation(nextId++, recommendation.productId(), recommendation.storeId(), recommendation.supplierId(),
							recommendation.suggestedQuantity(), recommendation.orderDeadlineDate(), recommendation.justification(),
							recommendation.status(), recommendation.generationDate(), recommendation.feedbackComment(), recommendation.feedbackDate())
					: recommendation;
			recommendations.put(toStore.recommendationId(), toStore);
			return toStore;
		}

		@Override
		public Optional<Recommendation> findById(Long recommendationId) {
			return Optional.ofNullable(recommendations.get(recommendationId));
		}

		@Override
		public List<Recommendation> findByFilters(Long storeId, Long supplierId, RecommendationStatus status) {
			return recommendations.values().stream()
					.filter(r -> r.storeId().equals(storeId))
					.filter(r -> supplierId == null || supplierId.equals(r.supplierId()))
					.filter(r -> status == null || status == r.status())
					.toList();
		}
	}
}

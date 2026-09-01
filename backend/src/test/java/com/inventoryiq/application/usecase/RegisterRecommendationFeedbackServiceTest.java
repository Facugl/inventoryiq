package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.RecommendationResult;
import com.inventoryiq.application.port.in.RegisterRecommendationFeedbackCommand;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.RecommendationRepository;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.exception.RecommendationNotFoundException;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Recommendation;
import com.inventoryiq.domain.model.RecommendationStatus;
import com.inventoryiq.domain.model.vo.LeadTime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegisterRecommendationFeedbackServiceTest {

	private static final Long STORE_ID = 1L;
	private static final LocalDate FEEDBACK_DATE = LocalDate.parse("2026-08-03");

	@Test
	void registersFeedbackAndReturnsTheUpdatedRecommendationEnrichedWithProductData() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		FakeProductRepository products = new FakeProductRepository();
		products.add(new Product(1001L, "SKU-1001", "Producto 1001", 10L, 5L, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true));
		recommendations.add(new Recommendation(1L, 1001L, STORE_ID, 5L, 50, LocalDate.parse("2026-08-05"),
				"justificación", RecommendationStatus.PENDING, LocalDate.parse("2026-08-01"), null, null));

		var service = new RegisterRecommendationFeedbackService(recommendations, products);

		RecommendationResult result = service.execute(
				new RegisterRecommendationFeedbackCommand(1L, RecommendationStatus.APPLIED, "comprado", FEEDBACK_DATE));

		assertEquals(RecommendationStatus.APPLIED, result.status());
		assertEquals("comprado", result.feedbackComment());
		assertEquals(FEEDBACK_DATE, result.feedbackDate());
		assertEquals("SKU-1001", result.sku());
	}

	@Test
	void throwsRecommendationNotFoundWhenTheIdDoesNotExist() {
		var service = new RegisterRecommendationFeedbackService(new FakeRecommendationRepository(), new FakeProductRepository());

		assertThrows(RecommendationNotFoundException.class, () -> service.execute(
				new RegisterRecommendationFeedbackCommand(999L, RecommendationStatus.APPLIED, null, FEEDBACK_DATE)));
	}

	@Test
	void propagatesTheDomainRuleWhenTheRecommendationIsAlreadyResolved() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		recommendations.add(new Recommendation(1L, 1001L, STORE_ID, 5L, 50, LocalDate.parse("2026-08-05"),
				"justificación", RecommendationStatus.DISCARDED, LocalDate.parse("2026-08-01"), "ya resuelta", FEEDBACK_DATE));

		var service = new RegisterRecommendationFeedbackService(recommendations, new FakeProductRepository());

		assertThrows(InvalidDomainDataException.class, () -> service.execute(
				new RegisterRecommendationFeedbackCommand(1L, RecommendationStatus.APPLIED, null, FEEDBACK_DATE)));
	}

	// ---- helpers ----

	private static class FakeRecommendationRepository implements RecommendationRepository {
		private final Map<Long, Recommendation> recommendations = new HashMap<>();

		void add(Recommendation recommendation) {
			recommendations.put(recommendation.recommendationId(), recommendation);
		}

		@Override
		public Recommendation save(Recommendation recommendation) {
			recommendations.put(recommendation.recommendationId(), recommendation);
			return recommendation;
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

	private static class FakeProductRepository implements ProductRepository {
		private final List<Product> products = new ArrayList<>();

		void add(Product product) {
			products.add(product);
		}

		@Override
		public List<Product> findAllActive() {
			return products.stream().filter(Product::active).toList();
		}

		@Override
		public Optional<Product> findById(Long productId) {
			return products.stream().filter(p -> p.productId().equals(productId)).findFirst();
		}
	}
}

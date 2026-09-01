package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.ListRecommendationsQuery;
import com.inventoryiq.application.port.in.RecommendationResult;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.RecommendationRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListRecommendationsServiceTest {

	private static final Long STORE_ID = 1L;

	@Test
	void enrichesEachRecommendationWithProductData() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		FakeProductRepository products = new FakeProductRepository();
		products.add(product(1001L, 10L, 5L));
		recommendations.add(recommendation(1L, 1001L, 5L, RecommendationStatus.PENDING));

		var service = new ListRecommendationsService(recommendations, products);

		List<RecommendationResult> results = service.execute(new ListRecommendationsQuery(STORE_ID, null, null, null));

		assertEquals(1, results.size());
		RecommendationResult result = results.get(0);
		assertEquals("SKU-1001", result.sku());
		assertEquals(10L, result.categoryId());
	}

	@Test
	void filtersByCategoryIdInMemory() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		FakeProductRepository products = new FakeProductRepository();
		products.add(product(1001L, 10L, 5L));
		products.add(product(1002L, 20L, 5L));
		recommendations.add(recommendation(1L, 1001L, 5L, RecommendationStatus.PENDING));
		recommendations.add(recommendation(2L, 1002L, 5L, RecommendationStatus.PENDING));

		var service = new ListRecommendationsService(recommendations, products);

		List<RecommendationResult> results = service.execute(new ListRecommendationsQuery(STORE_ID, 10L, null, null));

		assertEquals(1, results.size());
		assertEquals(1001L, results.get(0).productId());
	}

	@Test
	void dropsARecommendationWhoseProductNoLongerExistsInTheCatalog() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		FakeProductRepository products = new FakeProductRepository();
		recommendations.add(recommendation(1L, 9999L, 5L, RecommendationStatus.PENDING));

		var service = new ListRecommendationsService(recommendations, products);

		List<RecommendationResult> results = service.execute(new ListRecommendationsQuery(STORE_ID, null, null, null));

		assertTrue(results.isEmpty());
	}

	// ---- helpers ----

	private static Product product(Long productId, Long categoryId, Long supplierId) {
		return new Product(productId, "SKU-" + productId, "Producto " + productId, categoryId, supplierId, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true);
	}

	private static Recommendation recommendation(Long id, Long productId, Long supplierId, RecommendationStatus status) {
		return new Recommendation(id, productId, STORE_ID, supplierId, 50, LocalDate.parse("2026-08-05"),
				"justificación", status, LocalDate.parse("2026-08-01"), null, null);
	}

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

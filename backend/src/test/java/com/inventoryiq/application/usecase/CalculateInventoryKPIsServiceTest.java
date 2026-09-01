package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CalculateInventoryKPIsQuery;
import com.inventoryiq.application.port.in.DetectOverstockQuery;
import com.inventoryiq.application.port.in.DetectOverstockUseCase;
import com.inventoryiq.application.port.in.InventoryKPIsResult;
import com.inventoryiq.application.port.in.OverstockProductResult;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.RecommendationRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.domain.model.Category;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Recommendation;
import com.inventoryiq.domain.model.RecommendationStatus;
import com.inventoryiq.domain.model.Sale;
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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifica la orquestación de CalculateInventoryKPIsService con los
 * puertos de salida FAKEADOS en memoria (más un fake de
 * DetectOverstockUseCase, ya que este servicio lo reutiliza tal cual),
 * mismo criterio que los otros casos de uso de este proyecto.
 */
class CalculateInventoryKPIsServiceTest {

	private static final Long STORE_ID = 1L;
	private static final Long CATEGORY_ID = 10L;
	private static final Category CATEGORY = new Category(CATEGORY_ID, "Categoria Test", null, 30, 3);

	@Test
	void computesStockoutRateAndAverageCoverageAcrossTheCatalog() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		products.add(product(2001L));
		addDailySales(sales, 2001L, 10);
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 0)); // sin stock al toDate

		products.add(product(2002L));
		addDailySales(sales, 2002L, 10);
		addDailyInventory(inventory, 2002L, List.of(100, 90, 80, 70, 60, 50)); // coverage = 50/10 = 5

		var service = new CalculateInventoryKPIsService(
				products, categories, sales, inventory, new FakeDetectOverstockUseCase(List.of()), new FakeRecommendationRepository());

		InventoryKPIsResult result = service.execute(
				new CalculateInventoryKPIsQuery(STORE_ID, LocalDate.parse("2025-07-01"), LocalDate.parse("2025-07-10")));

		assertEquals(50.0, result.stockoutRate(), 1e-9); // 1 de 2 productos sin stock
		assertEquals(2.5, result.averageDaysOfCoverage(), 1e-9); // (0 + 5) / 2
	}

	@Test
	void returnsNullStockoutAndCoverageWhenNoProductHasEnoughHistory() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(product(2001L)); // sin ventas ni inventario cargados

		var service = new CalculateInventoryKPIsService(
				products, new FakeCategoryRepository(), new FakeSaleRepository(), new FakeInventoryRepository(),
				new FakeDetectOverstockUseCase(List.of()), new FakeRecommendationRepository());

		InventoryKPIsResult result = service.execute(
				new CalculateInventoryKPIsQuery(STORE_ID, LocalDate.parse("2025-07-01"), LocalDate.parse("2025-07-10")));

		assertNull(result.stockoutRate());
		assertNull(result.averageDaysOfCoverage());
	}

	@Test
	void immobilizedOverstockValueSumsAcrossEveryOverstockProduct() {
		FakeDetectOverstockUseCase overstock = new FakeDetectOverstockUseCase(List.of(
				new OverstockProductResult(3001L, "SKU-3001", "Producto 3001", STORE_ID, CATEGORY_ID, 500, 60.0, new BigDecimal("1000.50")),
				new OverstockProductResult(3002L, "SKU-3002", "Producto 3002", STORE_ID, CATEGORY_ID, 800, 90.0, new BigDecimal("2500.25"))));

		var service = new CalculateInventoryKPIsService(
				new FakeProductRepository(), new FakeCategoryRepository(), new FakeSaleRepository(), new FakeInventoryRepository(),
				overstock, new FakeRecommendationRepository());

		InventoryKPIsResult result = service.execute(
				new CalculateInventoryKPIsQuery(STORE_ID, LocalDate.parse("2025-07-01"), LocalDate.parse("2025-07-10")));

		assertEquals(new BigDecimal("3500.75"), result.immobilizedOverstockValue());
	}

	@Test
	void immobilizedOverstockValueIsZeroWhenThereIsNoOverstock() {
		var service = new CalculateInventoryKPIsService(
				new FakeProductRepository(), new FakeCategoryRepository(), new FakeSaleRepository(), new FakeInventoryRepository(),
				new FakeDetectOverstockUseCase(List.of()), new FakeRecommendationRepository());

		InventoryKPIsResult result = service.execute(
				new CalculateInventoryKPIsQuery(STORE_ID, LocalDate.parse("2025-07-01"), LocalDate.parse("2025-07-10")));

		assertEquals(BigDecimal.ZERO, result.immobilizedOverstockValue());
	}

	@Test
	void recommendationsFollowedRateIsMeasuredAgainstResolvedRecommendationsOnly() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		LocalDate from = LocalDate.parse("2025-07-01");
		LocalDate to = LocalDate.parse("2025-07-10");
		recommendations.add(recommendation(1L, RecommendationStatus.APPLIED, LocalDate.parse("2025-07-02")));
		recommendations.add(recommendation(2L, RecommendationStatus.APPLIED, LocalDate.parse("2025-07-03")));
		recommendations.add(recommendation(3L, RecommendationStatus.DISCARDED, LocalDate.parse("2025-07-04")));
		recommendations.add(recommendation(4L, RecommendationStatus.PENDING, LocalDate.parse("2025-07-05")));
		recommendations.add(recommendation(5L, RecommendationStatus.PENDING, LocalDate.parse("2025-07-06")));
		recommendations.add(recommendation(6L, RecommendationStatus.PENDING, LocalDate.parse("2025-07-07")));

		var service = new CalculateInventoryKPIsService(
				new FakeProductRepository(), new FakeCategoryRepository(), new FakeSaleRepository(), new FakeInventoryRepository(),
				new FakeDetectOverstockUseCase(List.of()), recommendations);

		InventoryKPIsResult result = service.execute(new CalculateInventoryKPIsQuery(STORE_ID, from, to));

		// 2 APPLIED de 3 resueltas (2 APPLIED + 1 DISCARDED), las 3 PENDING no cuentan.
		assertEquals(200.0 / 3, result.recommendationsFollowedRate(), 1e-9);
	}

	@Test
	void returnsNullRecommendationsFollowedRateWhenNoneWasResolvedInThePeriod() {
		FakeRecommendationRepository recommendations = new FakeRecommendationRepository();
		recommendations.add(recommendation(1L, RecommendationStatus.PENDING, LocalDate.parse("2025-07-05")));

		var service = new CalculateInventoryKPIsService(
				new FakeProductRepository(), new FakeCategoryRepository(), new FakeSaleRepository(), new FakeInventoryRepository(),
				new FakeDetectOverstockUseCase(List.of()), recommendations);

		InventoryKPIsResult result = service.execute(
				new CalculateInventoryKPIsQuery(STORE_ID, LocalDate.parse("2025-07-01"), LocalDate.parse("2025-07-10")));

		assertNull(result.recommendationsFollowedRate());
	}

	@Test
	void inventoryTurnoverIsCogsOverAverageInventoryInThePeriod() {
		FakeProductRepository products = new FakeProductRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();

		Product product = new Product(4001L, "SKU-4001", "Producto 4001", CATEGORY_ID, 1L, "UN",
				new BigDecimal("10.00"), new BigDecimal("15.00"), new LeadTime(3), true);
		products.add(product);

		LocalDate from = LocalDate.parse("2025-07-01");
		LocalDate to = LocalDate.parse("2025-07-02");
		sales.add(new Sale(1L, from, 4001L, STORE_ID, 5, new BigDecimal("50.00")));
		sales.add(new Sale(2L, to, 4001L, STORE_ID, 5, new BigDecimal("50.00")));
		inventory.add(new Inventory(1L, from, 4001L, STORE_ID, 20, 0));
		inventory.add(new Inventory(2L, to, 4001L, STORE_ID, 30, 0));

		var service = new CalculateInventoryKPIsService(
				products, new FakeCategoryRepository(), sales, inventory,
				new FakeDetectOverstockUseCase(List.of()), new FakeRecommendationRepository());

		InventoryKPIsResult result = service.execute(new CalculateInventoryKPIsQuery(STORE_ID, from, to));

		// COGS = 10 unidades * 10.00 = 100.00; inventario promedio = (200 + 300) / 2 = 250.00.
		assertEquals(0.4, result.inventoryTurnover(), 1e-9);
	}

	@Test
	void returnsNullInventoryTurnoverWhenThereAreNoSnapshotsInThePeriod() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(product(4001L));

		var service = new CalculateInventoryKPIsService(
				products, new FakeCategoryRepository(), new FakeSaleRepository(), new FakeInventoryRepository(),
				new FakeDetectOverstockUseCase(List.of()), new FakeRecommendationRepository());

		InventoryKPIsResult result = service.execute(
				new CalculateInventoryKPIsQuery(STORE_ID, LocalDate.parse("2025-07-01"), LocalDate.parse("2025-07-10")));

		assertNull(result.inventoryTurnover());
	}

	// ---- helpers ----

	private static Product product(Long id) {
		return new Product(id, "SKU-" + id, "Producto " + id, CATEGORY_ID, 1L, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true);
	}

	private static Recommendation recommendation(Long id, RecommendationStatus status, LocalDate generationDate) {
		return new Recommendation(id, 5001L, STORE_ID, 1L, 10, generationDate.plusDays(2), "justificación",
				status, generationDate, status == RecommendationStatus.PENDING ? null : "feedback", status == RecommendationStatus.PENDING ? null : generationDate);
	}

	private static void addDailySales(FakeSaleRepository repo, Long productId, int unitsPerDay) {
		LocalDate date = LocalDate.parse("2025-07-06");
		for (int i = 0; i < 5; i++) {
			repo.add(new Sale((long) (100000 + productId + i), date.plusDays(i), productId, STORE_ID, unitsPerDay, BigDecimal.valueOf(unitsPerDay * 10L)));
		}
	}

	private static void addDailyInventory(FakeInventoryRepository repo, Long productId, List<Integer> closingStocks) {
		LocalDate date = LocalDate.parse("2025-07-05");
		for (int i = 0; i < closingStocks.size(); i++) {
			repo.add(new Inventory((long) (200000 + productId + i), date.plusDays(i), productId, STORE_ID, closingStocks.get(i), 0));
		}
	}

	private static class FakeProductRepository implements ProductRepository {
		private final Map<Long, Product> products = new HashMap<>();

		void add(Product product) {
			products.put(product.productId(), product);
		}

		@Override
		public List<Product> findAllActive() {
			return products.values().stream().filter(Product::active).toList();
		}

		@Override
		public Optional<Product> findById(Long productId) {
			return Optional.ofNullable(products.get(productId));
		}
	}

	private static class FakeCategoryRepository implements CategoryRepository {
		private final Map<Long, Category> categories = new HashMap<>();

		void add(Category category) {
			categories.put(category.categoryId(), category);
		}

		@Override
		public Optional<Category> findById(Long categoryId) {
			return Optional.ofNullable(categories.get(categoryId));
		}
	}

	private static class FakeSaleRepository implements SaleRepository {
		private final List<Sale> sales = new ArrayList<>();

		void add(Sale sale) {
			sales.add(sale);
		}

		@Override
		public List<Sale> findByProductAndStore(Long productId, Long storeId, LocalDate from, LocalDate to) {
			return sales.stream()
					.filter(s -> s.productId().equals(productId) && s.storeId().equals(storeId))
					.filter(s -> !s.date().isBefore(from) && !s.date().isAfter(to))
					.toList();
		}
	}

	private static class FakeInventoryRepository implements InventoryRepository {
		private final List<Inventory> snapshots = new ArrayList<>();

		void add(Inventory snapshot) {
			snapshots.add(snapshot);
		}

		@Override
		public Optional<Inventory> findLatestSnapshotAsOf(Long productId, Long storeId, LocalDate asOfDate) {
			return snapshots.stream()
					.filter(i -> i.productId().equals(productId) && i.storeId().equals(storeId))
					.filter(i -> !i.snapshotDate().isAfter(asOfDate))
					.max((a, b) -> a.snapshotDate().compareTo(b.snapshotDate()));
		}

		@Override
		public List<Inventory> findSnapshotsInRange(Long productId, Long storeId, LocalDate from, LocalDate to) {
			return snapshots.stream()
					.filter(i -> i.productId().equals(productId) && i.storeId().equals(storeId))
					.filter(i -> !i.snapshotDate().isBefore(from) && !i.snapshotDate().isAfter(to))
					.toList();
		}
	}

	private static class FakeRecommendationRepository implements RecommendationRepository {
		private final List<Recommendation> recommendations = new ArrayList<>();

		void add(Recommendation recommendation) {
			recommendations.add(recommendation);
		}

		@Override
		public Recommendation save(Recommendation recommendation) {
			recommendations.add(recommendation);
			return recommendation;
		}

		@Override
		public Optional<Recommendation> findById(Long recommendationId) {
			return recommendations.stream().filter(r -> r.recommendationId().equals(recommendationId)).findFirst();
		}

		@Override
		public List<Recommendation> findByFilters(
				Long storeId, Long supplierId, RecommendationStatus status,
				LocalDate generationDateFrom, LocalDate generationDateTo) {
			return recommendations.stream()
					.filter(r -> r.storeId().equals(storeId))
					.filter(r -> supplierId == null || supplierId.equals(r.supplierId()))
					.filter(r -> status == null || status == r.status())
					.filter(r -> generationDateFrom == null || !r.generationDate().isBefore(generationDateFrom))
					.filter(r -> generationDateTo == null || !r.generationDate().isAfter(generationDateTo))
					.toList();
		}
	}

	private static class FakeDetectOverstockUseCase implements DetectOverstockUseCase {
		private final List<OverstockProductResult> results;

		FakeDetectOverstockUseCase(List<OverstockProductResult> results) {
			this.results = results;
		}

		@Override
		public List<OverstockProductResult> execute(DetectOverstockQuery query) {
			return results;
		}
	}
}

package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.DetectOverstockQuery;
import com.inventoryiq.application.port.in.OverstockProductResult;
import com.inventoryiq.application.port.in.OverstockSortBy;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.domain.model.Category;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
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

/**
 * Verifica la orquestación de DetectOverstockService con los 4 puertos de
 * salida FAKEADOS en memoria (no los adaptadores CSV), mismo criterio que
 * GetCriticalProductsServiceTest.
 */
class DetectOverstockServiceTest {

	private static final Long STORE_ID = 1L;
	private static final Long CATEGORY_ID = 10L;
	private static final Category CATEGORY = new Category(CATEGORY_ID, "Categoria Test", null, 30, 3);

	private static final LocalDate REFERENCE_DATE = LocalDate.parse("2025-07-10");
	private static final int WINDOW_DAYS = 5; // windowStart = 2025-07-06

	@Test
	void includesOverstockProductAndExcludesProductBelowReorderPoint() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		// Producto 3001: ADS=1, stock=100 -> cobertura=100 días > umbral(30) -> OVERSTOCK.
		products.add(product(3001L, "A-3001", new BigDecimal("100.00")));
		addDailySales(sales, 3001L, 1, new BigDecimal("100"));
		addDailyInventory(inventory, 3001L, List.of(105, 104, 103, 102, 101, 100));

		// Producto 2001: ADS=10, stock=50 -> por debajo del punto de pedido (60) -> REQUIRES_REPLENISHMENT, no sobrestock.
		products.add(product(2001L, "A-2001", new BigDecimal("100.00")));
		addDailySales(sales, 2001L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		var service = new DetectOverstockService(products, categories, sales, inventory);

		List<OverstockProductResult> results = service.execute(
				DetectOverstockQuery.of(STORE_ID, null, REFERENCE_DATE, OverstockSortBy.IMMOBILIZED_VALUE));

		assertEquals(1, results.size());
		OverstockProductResult result = results.get(0);
		assertEquals(3001L, result.productId());
		assertEquals(100, result.currentStock());
		assertEquals(100.0, result.currentDaysOfCoverage(), 0.0001);
		assertEquals(new BigDecimal("10000.00"), result.immobilizedValue()); // 100 unidades * 100.00
	}

	@Test
	void sortByChangesTheOrderBetweenTwoOverstockProducts() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		// Producto 7001 ("vino"): poco stock relativo, pero caro -> mucho valor inmovilizado,
		// cobertura moderada (40 días).
		products.add(product(7001L, "A-7001", new BigDecimal("5000.00")));
		addDailySales(sales, 7001L, 5, new BigDecimal("500"));
		addDailyInventory(inventory, 7001L, List.of(225, 220, 215, 210, 205, 200));

		// Producto 7002 ("fósforos"): mucho stock, barato -> poco valor inmovilizado,
		// pero cobertura extrema (200 días, casi no rota).
		products.add(product(7002L, "A-7002", new BigDecimal("50.00")));
		addDailySales(sales, 7002L, 25, new BigDecimal("2500"));
		addDailyInventory(inventory, 7002L, List.of(5125, 5100, 5075, 5050, 5025, 5000));

		var service = new DetectOverstockService(products, categories, sales, inventory);

		List<OverstockProductResult> byValue = service.execute(
				DetectOverstockQuery.of(STORE_ID, null, REFERENCE_DATE, OverstockSortBy.IMMOBILIZED_VALUE));
		assertEquals(List.of(7001L, 7002L), byValue.stream().map(OverstockProductResult::productId).toList());

		List<OverstockProductResult> byCoverage = service.execute(
				DetectOverstockQuery.of(STORE_ID, null, REFERENCE_DATE, OverstockSortBy.DAYS_OF_COVERAGE));
		assertEquals(List.of(7002L, 7001L), byCoverage.stream().map(OverstockProductResult::productId).toList());
	}

	@Test
	void filtersByCategoryId() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);
		Category otherCategory = new Category(20L, "Otra Categoria", null, 30, 3);
		categories.add(otherCategory);

		products.add(product(3001L, "A-3001", CATEGORY_ID, new BigDecimal("100.00")));
		addDailySales(sales, 3001L, 1, new BigDecimal("100"));
		addDailyInventory(inventory, 3001L, List.of(105, 104, 103, 102, 101, 100));

		// Categoría distinta: se filtra antes de evaluarse.
		products.add(product(3002L, "A-3002", 20L, new BigDecimal("100.00")));

		var service = new DetectOverstockService(products, categories, sales, inventory);

		List<OverstockProductResult> results = service.execute(
				new DetectOverstockQuery(STORE_ID, CATEGORY_ID, REFERENCE_DATE, WINDOW_DAYS, OverstockSortBy.IMMOBILIZED_VALUE));

		assertEquals(1, results.size());
		assertEquals(3001L, results.get(0).productId());
	}

	// ---- helpers ----

	private static Product product(Long id, String sku, BigDecimal costPrice) {
		return product(id, sku, CATEGORY_ID, costPrice);
	}

	private static Product product(Long id, String sku, Long categoryId, BigDecimal costPrice) {
		return new Product(id, sku, "Producto " + sku, categoryId, 1L, "UN",
				costPrice, costPrice.add(new BigDecimal("50.00")), new LeadTime(3), true);
	}

	private static void addDailySales(FakeSaleRepository repo, Long productId, int unitsPerDay, BigDecimal amountPerDay) {
		LocalDate date = LocalDate.parse("2025-07-06");
		for (int i = 0; i < 5; i++) {
			repo.add(new Sale((long) (100000 + productId + i), date.plusDays(i), productId, STORE_ID, unitsPerDay, amountPerDay));
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
}

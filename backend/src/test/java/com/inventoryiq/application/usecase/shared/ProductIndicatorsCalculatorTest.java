package com.inventoryiq.application.usecase.shared;

import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.domain.model.Category;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.ProductStatus;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductIndicatorsCalculatorTest {

	private static final Long STORE_ID = 1L;
	private static final Long CATEGORY_ID = 10L;
	private static final Category CATEGORY = new Category(CATEGORY_ID, "Categoria Test", null, 30, 3);

	private static final LocalDate REFERENCE_DATE = LocalDate.parse("2025-07-10");
	private static final int WINDOW_DAYS = 5; // windowStart = 2025-07-06

	@Test
	void computesIndicatorsAndRequiresReplenishmentWhenStockIsBelowReorderPoint() {
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);
		Product product = product(2001L, 3);
		List<Sale> sales = dailySales(product.productId(), 10, new BigDecimal("1300"));
		addDailyInventory(inventory, product.productId(), List.of(100, 90, 80, 70, 60, 50));

		var calculator = new ProductIndicatorsCalculator(categories, inventory);
		LocalDate windowStart = REFERENCE_DATE.minusDays(WINDOW_DAYS - 1);

		Optional<ProductIndicatorsCalculator.ProductIndicators> result =
				calculator.calculate(product, STORE_ID, windowStart, REFERENCE_DATE, sales);

		assertTrue(result.isPresent());
		ProductIndicatorsCalculator.ProductIndicators indicators = result.get();
		assertEquals(10.0, indicators.ads(), 0.0001);
		assertEquals(50, indicators.currentStock());
		assertEquals(30.0, indicators.safetyStock().units(), 0.0001);
		assertEquals(60.0, indicators.reorderPoint().units(), 0.0001);
		assertEquals(5.0, indicators.currentDaysOfCoverage(), 0.0001);
		assertEquals(ProductStatus.REQUIRES_REPLENISHMENT, indicators.status());
	}

	@Test
	void detectsOverstockWhenCoverageExceedsCategoryThreshold() {
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);
		Product product = product(3001L, 3);
		List<Sale> sales = dailySales(product.productId(), 1, new BigDecimal("100"));
		addDailyInventory(inventory, product.productId(), List.of(105, 104, 103, 102, 101, 100));

		var calculator = new ProductIndicatorsCalculator(categories, inventory);
		LocalDate windowStart = REFERENCE_DATE.minusDays(WINDOW_DAYS - 1);

		Optional<ProductIndicatorsCalculator.ProductIndicators> result =
				calculator.calculate(product, STORE_ID, windowStart, REFERENCE_DATE, sales);

		assertTrue(result.isPresent());
		ProductIndicatorsCalculator.ProductIndicators indicators = result.get();
		assertEquals(1.0, indicators.ads(), 0.0001);
		assertEquals(100, indicators.currentStock());
		assertEquals(100.0, indicators.currentDaysOfCoverage(), 0.0001); // 100/1 > umbral(30)
		assertEquals(ProductStatus.OVERSTOCK, indicators.status());
	}

	@Test
	void returnsEmptyWhenThereIsNoSalesHistoryInTheWindow() {
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);
		Product product = product(4001L, 3);
		LocalDate windowStart = REFERENCE_DATE.minusDays(WINDOW_DAYS - 1);

		Optional<ProductIndicatorsCalculator.ProductIndicators> result = new ProductIndicatorsCalculator(categories, inventory)
				.calculate(product, STORE_ID, windowStart, REFERENCE_DATE, List.of());

		assertTrue(result.isEmpty());
	}

	@Test
	void returnsEmptyWhenThereIsNoInventorySnapshot() {
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);
		Product product = product(5001L, 3);
		List<Sale> sales = dailySales(product.productId(), 10, new BigDecimal("1300"));
		LocalDate windowStart = REFERENCE_DATE.minusDays(WINDOW_DAYS - 1);

		Optional<ProductIndicatorsCalculator.ProductIndicators> result = new ProductIndicatorsCalculator(categories, inventory)
				.calculate(product, STORE_ID, windowStart, REFERENCE_DATE, sales);

		assertTrue(result.isEmpty());
	}

	@Test
	void returnsEmptyWhenTheCategoryDoesNotExist() {
		FakeCategoryRepository categories = new FakeCategoryRepository(); // sin categorías cargadas
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		Product product = product(6001L, 3);
		List<Sale> sales = dailySales(product.productId(), 10, new BigDecimal("1300"));
		addDailyInventory(inventory, product.productId(), List.of(100, 90, 80, 70, 60, 50));
		LocalDate windowStart = REFERENCE_DATE.minusDays(WINDOW_DAYS - 1);

		Optional<ProductIndicatorsCalculator.ProductIndicators> result = new ProductIndicatorsCalculator(categories, inventory)
				.calculate(product, STORE_ID, windowStart, REFERENCE_DATE, sales);

		assertTrue(result.isEmpty());
	}

	// ---- helpers ----

	private static Product product(Long id, int leadTimeDays) {
		return new Product(id, "SKU-" + id, "Producto " + id, CATEGORY_ID, 1L, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(leadTimeDays), true);
	}

	private static List<Sale> dailySales(Long productId, int unitsPerDay, BigDecimal amountPerDay) {
		List<Sale> sales = new ArrayList<>();
		LocalDate date = LocalDate.parse("2025-07-06");
		for (int i = 0; i < WINDOW_DAYS; i++) {
			sales.add(new Sale((long) (100000 + i), date.plusDays(i), productId, STORE_ID, unitsPerDay, amountPerDay));
		}
		return sales;
	}

	private static void addDailyInventory(FakeInventoryRepository repo, Long productId, List<Integer> closingStocks) {
		LocalDate date = LocalDate.parse("2025-07-05");
		for (int i = 0; i < closingStocks.size(); i++) {
			repo.add(new Inventory((long) (200000 + i), date.plusDays(i), productId, STORE_ID, closingStocks.get(i), 0));
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

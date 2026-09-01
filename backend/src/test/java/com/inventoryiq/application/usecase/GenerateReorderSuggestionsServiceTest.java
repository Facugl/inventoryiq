package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.GenerateReorderSuggestionsQuery;
import com.inventoryiq.application.port.in.ReorderSuggestionResult;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica la orquestación de GenerateReorderSuggestionsService con los 4
 * puertos de salida FAKEADOS en memoria (no los adaptadores CSV), mismo
 * criterio que los otros casos de uso de este proyecto.
 */
class GenerateReorderSuggestionsServiceTest {

	private static final Long STORE_ID = 1L;
	private static final Long CATEGORY_ID = 10L;
	private static final Long SUPPLIER_ID = 1L;
	private static final Category CATEGORY = new Category(CATEGORY_ID, "Categoria Test", null, 30, 3);

	private static final LocalDate REFERENCE_DATE = LocalDate.parse("2025-07-10");

	@Test
	void suggestsAQuantityAndDeadlineForAProductBelowReorderPoint() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		// ADS=10, safetyStock=30, ROP=60, currentStock=50 -> REQUIRES_REPLENISHMENT.
		products.add(product(2001L, SUPPLIER_ID));
		addDailySales(sales, 2001L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		var service = new GenerateReorderSuggestionsService(products, categories, sales, inventory);

		List<ReorderSuggestionResult> results = service.execute(
				new GenerateReorderSuggestionsQuery(STORE_ID, null, null, REFERENCE_DATE));

		assertEquals(1, results.size());
		ReorderSuggestionResult result = results.get(0);
		assertEquals(2001L, result.productId());
		assertEquals(SUPPLIER_ID, result.supplierId());
		// cantidad = ADS*15 - stock actual - en tránsito = 10*15 - 50 - 0 = 100
		assertEquals(100, result.suggestedQuantity());
		// deadline = referenceDate + floor((50-30)/10) = referenceDate + 2 días
		assertEquals(REFERENCE_DATE.plusDays(2), result.orderDeadlineDate());
		assertFalse(result.justification().isBlank());
	}

	@Test
	void excludesProductAboveReorderPoint() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		// ADS=1, ROP=6, currentStock=20 -> NORMAL, no dispara reposición.
		products.add(product(2002L, SUPPLIER_ID));
		addDailySales(sales, 2002L, 1, new BigDecimal("700"));
		addDailyInventory(inventory, 2002L, List.of(25, 24, 23, 22, 21, 20));

		var service = new GenerateReorderSuggestionsService(products, categories, sales, inventory);

		List<ReorderSuggestionResult> results = service.execute(
				new GenerateReorderSuggestionsQuery(STORE_ID, null, null, REFERENCE_DATE));

		assertTrue(results.isEmpty());
	}

	@Test
	void excludesProductWithInsufficientSalesHistory() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		products.add(product(2001L, SUPPLIER_ID));
		addDailySales(sales, 2001L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		// Sin ninguna venta ni snapshot registrado.
		products.add(product(3001L, SUPPLIER_ID));

		var service = new GenerateReorderSuggestionsService(products, categories, sales, inventory);

		List<ReorderSuggestionResult> results = service.execute(
				new GenerateReorderSuggestionsQuery(STORE_ID, null, null, REFERENCE_DATE));

		assertEquals(1, results.size());
		assertEquals(2001L, results.get(0).productId());
	}

	@Test
	void filtersByCategoryId() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);
		categories.add(new Category(20L, "Otra Categoria", null, 30, 3));

		products.add(new Product(2001L, "A-2001", "Producto A-2001", CATEGORY_ID, SUPPLIER_ID, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true));
		addDailySales(sales, 2001L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		products.add(new Product(2002L, "A-2002", "Producto A-2002", 20L, SUPPLIER_ID, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true));

		var service = new GenerateReorderSuggestionsService(products, categories, sales, inventory);

		List<ReorderSuggestionResult> results = service.execute(
				new GenerateReorderSuggestionsQuery(STORE_ID, CATEGORY_ID, null, REFERENCE_DATE));

		assertEquals(1, results.size());
		assertEquals(2001L, results.get(0).productId());
	}

	@Test
	void filtersBySupplierId() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		products.add(product(2001L, 1L));
		addDailySales(sales, 2001L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		products.add(product(2003L, 2L));
		addDailySales(sales, 2003L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 2003L, List.of(100, 90, 80, 70, 60, 50));

		var service = new GenerateReorderSuggestionsService(products, categories, sales, inventory);

		List<ReorderSuggestionResult> results = service.execute(
				new GenerateReorderSuggestionsQuery(STORE_ID, null, 1L, REFERENCE_DATE));

		assertEquals(1, results.size());
		assertEquals(2001L, results.get(0).productId());
	}

	// ---- helpers ----

	private static Product product(Long id, Long supplierId) {
		return new Product(id, "SKU-" + id, "Producto " + id, CATEGORY_ID, supplierId, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true);
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

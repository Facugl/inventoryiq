package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CriticalProductResult;
import com.inventoryiq.application.port.in.GetCriticalProductsQuery;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.domain.model.Category;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.vo.LeadTime;
import com.inventoryiq.domain.service.CriticalityEvaluator;
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

/**
 * Verifica la orquestación de GetCriticalProductsService con los 4
 * puertos de salida FAKEADOS en memoria (no los adaptadores CSV): esto
 * prueba que la lógica de aplicación es correcta de forma completamente
 * aislada de infraestructura, y que el reemplazo de CSV por Postgres el
 * día de mañana no debería requerir cambiar una sola línea de esta clase.
 */
class GetCriticalProductsServiceTest {

	private static final Long STORE_ID = 1L;
	private static final Long CATEGORY_ID = 10L;
	private static final Category CATEGORY = new Category(CATEGORY_ID, "Categoria Test", null, 30, 3);
	private static final CriticalityEvaluator.CriticalityWeights EQUAL_WEIGHTS =
			new CriticalityEvaluator.CriticalityWeights(1.0 / 3, 1.0 / 3, 1.0 / 3);

	private static final LocalDate REFERENCE_DATE = LocalDate.parse("2025-07-10");
	private static final int WINDOW_DAYS = 5; // windowStart = 2025-07-06

	@Test
	void includesProductRequiringReplenishmentAndExcludesNormalProduct() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		// Producto 2001: termina con stock 50, ADS=10, ROP=60 -> REQUIRES_REPLENISHMENT.
		// Domina el valor de venta (65%) -> clase ABC A.
		products.add(product(2001L, "A-2001", 3));
		addDailySales(sales, 2001L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		// Producto 2002: ADS=1, currentStock=20 muy por encima del ROP(6) y de la
		// cobertura máxima (20 días < 30) -> NORMAL, debe quedar excluido.
		products.add(product(2002L, "A-2002", 3));
		addDailySales(sales, 2002L, 1, new BigDecimal("700"));
		addDailyInventory(inventory, 2002L, List.of(25, 24, 23, 22, 21, 20));

		var service = new GetCriticalProductsService(products, categories, sales, inventory, EQUAL_WEIGHTS);

		List<CriticalProductResult> results = service.execute(
				new GetCriticalProductsQuery(STORE_ID, null, null, REFERENCE_DATE, WINDOW_DAYS));

		assertEquals(1, results.size());
		CriticalProductResult result = results.get(0);
		assertEquals(2001L, result.productId());
		assertEquals(ProductStatus.REQUIRES_REPLENISHMENT, result.status());
		assertEquals(50, result.currentStock());
		assertEquals(60.0, result.reorderPoint().units(), 0.0001);
		assertEquals(5.0, result.currentDaysOfCoverage(), 0.0001);
		// abcValue(A)=1.0, coverageFactor clamp(1-5/3)=0, stockout=0 -> score = 1/3*100 = 33.33...
		assertEquals(100.0 / 3, result.criticalityLevel().score(), 0.0001);
	}

	@Test
	void ordersByCriticalityScoreDescendingAndAppliesLimit() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		// Producto 3001: termina en stock 0 -> CRITICAL garantizado (indicador de quiebre=1).
		products.add(product(3001L, "A-3001", 3));
		addDailySales(sales, 3001L, 5, new BigDecimal("500"));
		addDailyInventory(inventory, 3001L, List.of(50, 40, 30, 20, 10, 0));

		// Producto 3002: igual que el producto 2001 del test anterior -> REQUIRES_REPLENISHMENT, score menor.
		products.add(product(3002L, "A-3002", 3));
		addDailySales(sales, 3002L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 3002L, List.of(100, 90, 80, 70, 60, 50));

		var service = new GetCriticalProductsService(products, categories, sales, inventory, EQUAL_WEIGHTS);

		List<CriticalProductResult> unlimited = service.execute(
				new GetCriticalProductsQuery(STORE_ID, null, null, REFERENCE_DATE, WINDOW_DAYS));

		assertEquals(2, unlimited.size());
		assertEquals(3001L, unlimited.get(0).productId());
		assertEquals(3002L, unlimited.get(1).productId());
		assertTrue(unlimited.get(0).criticalityLevel().score() > unlimited.get(1).criticalityLevel().score());

		List<CriticalProductResult> limited = service.execute(
				new GetCriticalProductsQuery(STORE_ID, null, 1, REFERENCE_DATE, WINDOW_DAYS));

		assertEquals(1, limited.size());
		assertEquals(3001L, limited.get(0).productId());
	}

	@Test
	void excludesProductWithInsufficientSalesHistoryWithoutFailingTheWholeBatch() {
		FakeProductRepository products = new FakeProductRepository();
		FakeCategoryRepository categories = new FakeCategoryRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();
		categories.add(CATEGORY);

		products.add(product(5001L, "A-5001", 3));
		addDailySales(sales, 5001L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 5001L, List.of(100, 90, 80, 70, 60, 50));

		// Producto sin ninguna venta ni snapshot registrado (por ejemplo, recién dado de alta).
		products.add(product(5002L, "A-5002", 3));

		var service = new GetCriticalProductsService(products, categories, sales, inventory, EQUAL_WEIGHTS);

		List<CriticalProductResult> results = service.execute(
				new GetCriticalProductsQuery(STORE_ID, null, null, REFERENCE_DATE, WINDOW_DAYS));

		assertEquals(1, results.size());
		assertEquals(5001L, results.get(0).productId());
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

		products.add(product(6001L, "A-6001", CATEGORY_ID, 3));
		addDailySales(sales, 6001L, 10, new BigDecimal("1300"));
		addDailyInventory(inventory, 6001L, List.of(100, 90, 80, 70, 60, 50));

		// Categoría distinta: se filtra antes de evaluarse, no necesita datos de ventas/inventario.
		products.add(product(6002L, "A-6002", 20L, 3));

		var service = new GetCriticalProductsService(products, categories, sales, inventory, EQUAL_WEIGHTS);

		List<CriticalProductResult> results = service.execute(
				new GetCriticalProductsQuery(STORE_ID, CATEGORY_ID, null, REFERENCE_DATE, WINDOW_DAYS));

		assertEquals(1, results.size());
		assertEquals(6001L, results.get(0).productId());
	}

	// ---- helpers ----

	private static Product product(Long id, String sku, int leadTimeDays) {
		return product(id, sku, CATEGORY_ID, leadTimeDays);
	}

	private static Product product(Long id, String sku, Long categoryId, int leadTimeDays) {
		return new Product(id, sku, "Producto " + sku, categoryId, 1L, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(leadTimeDays), true);
	}

	/** Carga 5 días de venta (2025-07-06 a 2025-07-10) con unidades/importe constantes. */
	private static void addDailySales(FakeSaleRepository repo, Long productId, int unitsPerDay, BigDecimal amountPerDay) {
		LocalDate date = LocalDate.parse("2025-07-06");
		for (int i = 0; i < 5; i++) {
			repo.add(new Sale((long) (100000 + i), date.plusDays(i), productId, STORE_ID, unitsPerDay, amountPerDay));
		}
	}

	/** Carga 6 snapshots de cierre (2025-07-05 a 2025-07-10): el primero es el día anterior a la ventana. */
	private static void addDailyInventory(FakeInventoryRepository repo, Long productId, List<Integer> closingStocks) {
		LocalDate date = LocalDate.parse("2025-07-05");
		for (int i = 0; i < closingStocks.size(); i++) {
			repo.add(new Inventory((long) (200000 + i), date.plusDays(i), productId, STORE_ID, closingStocks.get(i), 0));
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

package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.ClassifyProductsQuery;
import com.inventoryiq.application.port.in.ProductClassificationResult;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.domain.model.AbcClassification;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Product;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.XyzClassification;
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
 * Verifica la orquestación de ClassifyProductsService con los 3 puertos
 * de salida FAKEADOS en memoria (no los adaptadores CSV), mismo criterio
 * que los otros dos casos de uso de este slice.
 */
class ClassifyProductsServiceTest {

	private static final Long STORE_ID = 1L;
	private static final Long CATEGORY_ID = 10L;

	private static final LocalDate REFERENCE_DATE = LocalDate.parse("2025-07-10");
	private static final int WINDOW_DAYS = 5; // windowStart = 2025-07-06

	@Test
	void classifiesADominantStableProductAsAX() {
		FakeProductRepository products = new FakeProductRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();

		// Producto 2001: domina el valor de venta (65%) -> clase A.
		// Vende siempre 10 unidades por día, sin variación -> desvío 0 -> CV=0 -> clase X.
		products.add(product(2001L));
		addDailySales(sales, 2001L, List.of(10, 10, 10, 10, 10), new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		// Producto 2002: bajo valor de venta (35%) -> clase C. Sin uso en esta aserción.
		products.add(product(2002L));
		addDailySales(sales, 2002L, List.of(1, 1, 1, 1, 1), new BigDecimal("700"));
		addDailyInventory(inventory, 2002L, List.of(25, 24, 23, 22, 21, 20));

		var service = new ClassifyProductsService(products, sales, inventory);

		List<ProductClassificationResult> results = service.execute(
				new ClassifyProductsQuery(STORE_ID, null, REFERENCE_DATE, WINDOW_DAYS));

		ProductClassificationResult product2001 = results.stream()
				.filter(r -> r.productId().equals(2001L)).findFirst().orElseThrow();
		assertEquals(AbcClassification.A, product2001.abcClass());
		assertEquals(XyzClassification.X, product2001.xyzClass());
	}

	@Test
	void classifiesAnErraticLowValueProductAsCZ() {
		FakeProductRepository products = new FakeProductRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();

		products.add(product(2001L));
		addDailySales(sales, 2001L, List.of(10, 10, 10, 10, 10), new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		// Producto 2002: bajo valor de venta (100/6600 acumulado -> clase C) y ventas muy
		// irregulares (0, 20, 0, 20, 0) -> desvío alto respecto a la media -> CV>=1 -> clase Z.
		products.add(product(2002L));
		addDailySales(sales, 2002L, List.of(0, 20, 0, 20, 0), new BigDecimal("20"));
		addDailyInventory(inventory, 2002L, List.of(50, 50, 30, 30, 10, 10));

		var service = new ClassifyProductsService(products, sales, inventory);

		List<ProductClassificationResult> results = service.execute(
				new ClassifyProductsQuery(STORE_ID, null, REFERENCE_DATE, WINDOW_DAYS));

		ProductClassificationResult product2002 = results.stream()
				.filter(r -> r.productId().equals(2002L)).findFirst().orElseThrow();
		assertEquals(AbcClassification.C, product2002.abcClass());
		assertEquals(XyzClassification.Z, product2002.xyzClass());
	}

	@Test
	void excludesProductWithNoValidSalesHistory() {
		FakeProductRepository products = new FakeProductRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();

		products.add(product(2001L));
		addDailySales(sales, 2001L, List.of(10, 10, 10, 10, 10), new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		// Producto sin ninguna venta ni snapshot registrado.
		products.add(product(3001L));

		var service = new ClassifyProductsService(products, sales, inventory);

		List<ProductClassificationResult> results = service.execute(
				new ClassifyProductsQuery(STORE_ID, null, REFERENCE_DATE, WINDOW_DAYS));

		assertEquals(1, results.size());
		assertEquals(2001L, results.get(0).productId());
	}

	@Test
	void filtersByCategoryId() {
		FakeProductRepository products = new FakeProductRepository();
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();

		products.add(new Product(2001L, "A-2001", "Producto A-2001", CATEGORY_ID, 1L, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true));
		addDailySales(sales, 2001L, List.of(10, 10, 10, 10, 10), new BigDecimal("1300"));
		addDailyInventory(inventory, 2001L, List.of(100, 90, 80, 70, 60, 50));

		products.add(new Product(2002L, "A-2002", "Producto A-2002", 20L, 1L, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true));

		var service = new ClassifyProductsService(products, sales, inventory);

		List<ProductClassificationResult> results = service.execute(
				new ClassifyProductsQuery(STORE_ID, CATEGORY_ID, REFERENCE_DATE, WINDOW_DAYS));

		assertEquals(1, results.size());
		assertEquals(2001L, results.get(0).productId());
	}

	// ---- helpers ----

	private static Product product(Long id) {
		return new Product(id, "SKU-" + id, "Producto " + id, CATEGORY_ID, 1L, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true);
	}

	private static void addDailySales(FakeSaleRepository repo, Long productId, List<Integer> unitsPerDay, BigDecimal amountPerDay) {
		LocalDate date = LocalDate.parse("2025-07-06");
		for (int i = 0; i < unitsPerDay.size(); i++) {
			repo.add(new Sale((long) (100000 + productId + i), date.plusDays(i), productId, STORE_ID, unitsPerDay.get(i), amountPerDay));
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

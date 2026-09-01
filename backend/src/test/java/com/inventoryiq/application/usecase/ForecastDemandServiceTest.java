package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.DemandForecastPeriod;
import com.inventoryiq.application.port.in.ForecastDemandQuery;
import com.inventoryiq.application.port.in.ForecastDemandResult;
import com.inventoryiq.application.port.out.InventoryRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleRepository;
import com.inventoryiq.domain.exception.ProductNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica la orquestación de ForecastDemandService con los 3 puertos de
 * salida FAKEADOS en memoria (no los adaptadores CSV), mismo criterio que
 * los otros casos de uso de este proyecto.
 */
class ForecastDemandServiceTest {

	private static final Long STORE_ID = 1L;
	private static final Long CATEGORY_ID = 10L;
	private static final Long PRODUCT_ID = 2001L;

	@Test
	void throwsProductNotFoundWhenTheProductIdDoesNotExist() {
		var service = new ForecastDemandService(
				new FakeProductRepository(), new FakeSaleRepository(), new FakeInventoryRepository());

		assertThrows(ProductNotFoundException.class, () -> service.execute(
				new ForecastDemandQuery(PRODUCT_ID, STORE_ID, LocalDate.parse("2025-07-10"), 7)));
	}

	@Test
	void returnsNullBaseAdsAndNoPeriodsWhenThereIsNoSalesHistory() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(product());

		var service = new ForecastDemandService(products, new FakeSaleRepository(), new FakeInventoryRepository());

		ForecastDemandResult result = service.execute(
				new ForecastDemandQuery(PRODUCT_ID, STORE_ID, LocalDate.parse("2025-07-10"), 7));

		assertNull(result.baseAds());
		assertTrue(result.periods().isEmpty());
		assertEquals(PRODUCT_ID, result.productId());
	}

	@Test
	void appliesTheSeasonalIndexOfTheProjectedMonthWhenItHasDistinctHistory() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(product());
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();

		// Historial: noviembre a 10 u/día, diciembre a 20 u/día (continuo, sin quiebres).
		addContinuousInventory(inventory, LocalDate.parse("2025-10-31"), LocalDate.parse("2025-12-30"), 100);
		addSales(sales, LocalDate.parse("2025-11-01"), 5, 10);
		addSales(sales, LocalDate.parse("2025-12-01"), 5, 20);

		var service = new ForecastDemandService(products, sales, inventory);

		// referenceDate=2025-12-24, horizonte=7 -> único período: 2025-12-25 a 2025-12-31 (diciembre).
		ForecastDemandResult result = service.execute(
				new ForecastDemandQuery(PRODUCT_ID, STORE_ID, LocalDate.parse("2025-12-24"), 7));

		// baseAds = ADS corregido de todo el historial (nov+dic) = (5*10 + 5*20) / 10 = 15.
		assertEquals(15.0, result.baseAds(), 1e-9);
		assertEquals(1, result.periods().size());

		DemandForecastPeriod period = result.periods().get(0);
		assertEquals(LocalDate.parse("2025-12-25"), period.periodStart());
		assertEquals(LocalDate.parse("2025-12-31"), period.periodEnd());
		// índice de diciembre = ADS de diciembre (20) / ADS del período completo (15) = 1.3333...
		assertEquals(20.0 / 15.0, period.seasonalIndex(), 1e-9);
		// ADS proyectado = baseAds * índice = 15 * (20/15) = 20 -> coincide con el ADS histórico real de diciembre.
		assertEquals(20.0, period.projectedDailyAds(), 1e-6);
		assertEquals(140, period.projectedTotalDemand()); // round(20 * 7 días)
	}

	@Test
	void fallsBackToNoSeasonalAdjustmentWhenTheProjectedMonthHasNoHistory() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(product());
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();

		// Único mes con historial: julio, a 10 u/día.
		addContinuousInventory(inventory, LocalDate.parse("2025-07-05"), LocalDate.parse("2025-07-09"), 100);
		addSales(sales, LocalDate.parse("2025-07-06"), 5, 10);

		var service = new ForecastDemandService(products, sales, inventory);

		// referenceDate=2025-07-10, horizonte=35 días (5 períodos): jul11-17, jul18-24, jul25-31, ago1-7, ago8-14.
		ForecastDemandResult result = service.execute(
				new ForecastDemandQuery(PRODUCT_ID, STORE_ID, LocalDate.parse("2025-07-10"), 35));

		assertEquals(10.0, result.baseAds(), 1e-9);
		assertEquals(5, result.periods().size());

		DemandForecastPeriod augustPeriod = result.periods().get(3);
		assertEquals(LocalDate.parse("2025-08-01"), augustPeriod.periodStart());
		assertEquals(LocalDate.parse("2025-08-07"), augustPeriod.periodEnd());
		assertEquals(1.0, augustPeriod.seasonalIndex(), 1e-9); // agosto no tiene historial propio: sin ajuste
		assertEquals(10.0, augustPeriod.projectedDailyAds(), 1e-9); // == baseAds
	}

	@Test
	void thePartialLastPeriodCanBeShorterThanSevenDays() {
		FakeProductRepository products = new FakeProductRepository();
		products.add(product());
		FakeSaleRepository sales = new FakeSaleRepository();
		FakeInventoryRepository inventory = new FakeInventoryRepository();

		addContinuousInventory(inventory, LocalDate.parse("2025-07-05"), LocalDate.parse("2025-07-09"), 100);
		addSales(sales, LocalDate.parse("2025-07-06"), 5, 10);

		var service = new ForecastDemandService(products, sales, inventory);

		ForecastDemandResult result = service.execute(
				new ForecastDemandQuery(PRODUCT_ID, STORE_ID, LocalDate.parse("2025-07-10"), 10));

		assertEquals(2, result.periods().size());
		DemandForecastPeriod lastPeriod = result.periods().get(1);
		assertEquals(LocalDate.parse("2025-07-18"), lastPeriod.periodStart());
		assertEquals(LocalDate.parse("2025-07-20"), lastPeriod.periodEnd()); // solo 3 días restantes
		assertEquals(30, lastPeriod.projectedTotalDemand()); // round(10 * 3 días)
	}

	// ---- helpers ----

	private static Product product() {
		return new Product(PRODUCT_ID, "SKU-" + PRODUCT_ID, "Producto " + PRODUCT_ID, CATEGORY_ID, 1L, "UN",
				new BigDecimal("100.00"), new BigDecimal("150.00"), new LeadTime(3), true);
	}

	private static void addSales(FakeSaleRepository repo, LocalDate start, int days, int unitsPerDay) {
		for (int i = 0; i < days; i++) {
			LocalDate date = start.plusDays(i);
			repo.add(new Sale((long) (date.toEpochDay() * 1000 + PRODUCT_ID), date, PRODUCT_ID, STORE_ID,
					unitsPerDay, BigDecimal.valueOf(unitsPerDay * 10L)));
		}
	}

	private static void addContinuousInventory(FakeInventoryRepository repo, LocalDate from, LocalDate to, int stock) {
		LocalDate date = from;
		while (!date.isAfter(to)) {
			repo.add(new Inventory(date.toEpochDay() + PRODUCT_ID, date, PRODUCT_ID, STORE_ID, stock, 0));
			date = date.plusDays(1);
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

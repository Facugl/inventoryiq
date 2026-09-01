package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.AlertResult;
import com.inventoryiq.application.port.in.AlertType;
import com.inventoryiq.application.port.in.AlertSeverity;
import com.inventoryiq.application.port.in.CriticalProductResult;
import com.inventoryiq.application.port.in.DetectOverstockQuery;
import com.inventoryiq.application.port.in.DetectOverstockUseCase;
import com.inventoryiq.application.port.in.GenerateAlertsQuery;
import com.inventoryiq.application.port.in.GenerateAlertsUseCase;
import com.inventoryiq.application.port.in.GetCriticalProductsQuery;
import com.inventoryiq.application.port.in.GetCriticalProductsUseCase;
import com.inventoryiq.application.port.in.OverstockProductResult;
import com.inventoryiq.application.port.in.RecalculateProductStatusCommand;
import com.inventoryiq.application.port.in.RecalculateProductStatusResult;
import com.inventoryiq.application.port.in.RecalculateRecommendationsCommand;
import com.inventoryiq.application.port.in.RecalculateRecommendationsResult;
import com.inventoryiq.application.port.in.RecalculateRecommendationsUseCase;
import com.inventoryiq.application.port.in.StoreRecalculationSummary;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.model.Store;
import com.inventoryiq.domain.model.vo.CriticalityLevel;
import com.inventoryiq.domain.model.vo.ReorderPoint;
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
 * Verifica la orquestación de RecalculateProductStatusService con los 4
 * casos de uso que encadena y StoreRepository FAKEADOS en memoria, mismo
 * criterio que el resto del proyecto.
 */
class RecalculateProductStatusServiceTest {

	private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-02-01");

	@Test
	void processesOnlyTheGivenStoreWhenStoreIdIsProvided() {
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(1L, true);
		stores.add(2L, true);
		FakeGetCriticalProductsUseCase critical = new FakeGetCriticalProductsUseCase();
		FakeDetectOverstockUseCase overstock = new FakeDetectOverstockUseCase();
		FakeRecalculateRecommendationsUseCase recommendations = new FakeRecalculateRecommendationsUseCase();
		FakeGenerateAlertsUseCase alerts = new FakeGenerateAlertsUseCase();

		var service = new RecalculateProductStatusService(stores, critical, overstock, recommendations, alerts);

		RecalculateProductStatusResult result = service.execute(new RecalculateProductStatusCommand(1L, REFERENCE_DATE));

		assertEquals(1, result.storesProcessed());
		assertEquals(1L, result.perStore().get(0).storeId());
		assertEquals(List.of(1L), critical.calledForStores);
		assertEquals(List.of(1L), overstock.calledForStores);
		assertEquals(List.of(1L), recommendations.calledForStores);
		assertEquals(List.of(1L), alerts.calledForStores);
	}

	@Test
	void processesEveryActiveStoreWhenStoreIdIsOmitted() {
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(1L, true);
		stores.add(2L, true);
		stores.add(3L, false); // inactiva: no debe procesarse

		var service = new RecalculateProductStatusService(
				stores, new FakeGetCriticalProductsUseCase(), new FakeDetectOverstockUseCase(),
				new FakeRecalculateRecommendationsUseCase(), new FakeGenerateAlertsUseCase());

		RecalculateProductStatusResult result = service.execute(new RecalculateProductStatusCommand(null, REFERENCE_DATE));

		assertEquals(2, result.storesProcessed());
		assertEquals(List.of(1L, 2L), result.perStore().stream().map(StoreRecalculationSummary::storeId).toList());
	}

	@Test
	void aggregatesTheCountsReturnedByEachUseCase() {
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(1L, true);

		FakeGetCriticalProductsUseCase critical = new FakeGetCriticalProductsUseCase();
		critical.resultsByStore.put(1L, List.of(criticalProduct(), criticalProduct(), criticalProduct()));

		FakeDetectOverstockUseCase overstock = new FakeDetectOverstockUseCase();
		overstock.resultsByStore.put(1L, List.of(overstockProduct(), overstockProduct()));

		FakeRecalculateRecommendationsUseCase recommendations = new FakeRecalculateRecommendationsUseCase();
		recommendations.resultsByStore.put(1L, new RecalculateRecommendationsResult(5, 3, 2, 1));

		FakeGenerateAlertsUseCase alerts = new FakeGenerateAlertsUseCase();
		alerts.resultsByStore.put(1L, List.of(alert(), alert(), alert(), alert()));

		var service = new RecalculateProductStatusService(stores, critical, overstock, recommendations, alerts);

		RecalculateProductStatusResult result = service.execute(new RecalculateProductStatusCommand(1L, REFERENCE_DATE));

		StoreRecalculationSummary summary = result.perStore().get(0);
		assertEquals(3, summary.criticalProductsFound());
		assertEquals(2, summary.overstockProductsFound());
		assertEquals(4, summary.alertsGenerated());
		assertEquals(5, summary.recommendations().totalGenerated());
		assertEquals(1, summary.recommendations().autoDiscardedCount());
	}

	@Test
	void processesNoStoresWhenNoActiveStoreExists() {
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(1L, false);

		var service = new RecalculateProductStatusService(
				stores, new FakeGetCriticalProductsUseCase(), new FakeDetectOverstockUseCase(),
				new FakeRecalculateRecommendationsUseCase(), new FakeGenerateAlertsUseCase());

		RecalculateProductStatusResult result = service.execute(new RecalculateProductStatusCommand(null, REFERENCE_DATE));

		assertEquals(0, result.storesProcessed());
		assertTrue(result.perStore().isEmpty());
	}

	// ---- helpers ----

	private static CriticalProductResult criticalProduct() {
		return new CriticalProductResult(1001L, "SKU-1001", "Producto", 1L, 10L, 5,
				new ReorderPoint(20), 3.0, com.inventoryiq.domain.model.ProductStatus.CRITICAL, new CriticalityLevel(80));
	}

	private static OverstockProductResult overstockProduct() {
		return new OverstockProductResult(1002L, "SKU-1002", "Producto", 1L, 10L, 500, 90.0, new BigDecimal("1000.00"));
	}

	private static AlertResult alert() {
		return new AlertResult(1001L, "SKU-1001", "Producto", 1L, 10L, AlertType.STOCKOUT, AlertSeverity.HIGH, LocalDate.now());
	}

	private static class FakeStoreRepository implements StoreRepository {
		private final Map<Long, Store> stores = new HashMap<>();

		void add(Long storeId, boolean active) {
			stores.put(storeId, new Store(storeId, "Sucursal " + storeId, "dirección", active));
		}

		@Override
		public Optional<Store> findById(Long storeId) {
			return Optional.ofNullable(stores.get(storeId));
		}

		@Override
		public List<Store> findAllActive() {
			return stores.values().stream().filter(Store::active).sorted((a, b) -> a.storeId().compareTo(b.storeId())).toList();
		}
	}

	private static class FakeGetCriticalProductsUseCase implements GetCriticalProductsUseCase {
		final Map<Long, List<CriticalProductResult>> resultsByStore = new HashMap<>();
		final List<Long> calledForStores = new ArrayList<>();

		@Override
		public List<CriticalProductResult> execute(GetCriticalProductsQuery query) {
			calledForStores.add(query.storeId());
			return resultsByStore.getOrDefault(query.storeId(), List.of());
		}
	}

	private static class FakeDetectOverstockUseCase implements DetectOverstockUseCase {
		final Map<Long, List<OverstockProductResult>> resultsByStore = new HashMap<>();
		final List<Long> calledForStores = new ArrayList<>();

		@Override
		public List<OverstockProductResult> execute(DetectOverstockQuery query) {
			calledForStores.add(query.storeId());
			return resultsByStore.getOrDefault(query.storeId(), List.of());
		}
	}

	private static class FakeRecalculateRecommendationsUseCase implements RecalculateRecommendationsUseCase {
		final Map<Long, RecalculateRecommendationsResult> resultsByStore = new HashMap<>();
		final List<Long> calledForStores = new ArrayList<>();

		@Override
		public RecalculateRecommendationsResult execute(RecalculateRecommendationsCommand command) {
			calledForStores.add(command.storeId());
			return resultsByStore.getOrDefault(command.storeId(), new RecalculateRecommendationsResult(0, 0, 0, 0));
		}
	}

	private static class FakeGenerateAlertsUseCase implements GenerateAlertsUseCase {
		final Map<Long, List<AlertResult>> resultsByStore = new HashMap<>();
		final List<Long> calledForStores = new ArrayList<>();

		@Override
		public List<AlertResult> execute(GenerateAlertsQuery query) {
			calledForStores.add(query.storeId());
			return resultsByStore.getOrDefault(query.storeId(), List.of());
		}
	}
}

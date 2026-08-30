package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.AlertResult;
import com.inventoryiq.application.port.in.AlertSeverity;
import com.inventoryiq.application.port.in.AlertType;
import com.inventoryiq.application.port.in.CriticalProductResult;
import com.inventoryiq.application.port.in.DetectOverstockQuery;
import com.inventoryiq.application.port.in.DetectOverstockUseCase;
import com.inventoryiq.application.port.in.GenerateAlertsQuery;
import com.inventoryiq.application.port.in.GetCriticalProductsQuery;
import com.inventoryiq.application.port.in.GetCriticalProductsUseCase;
import com.inventoryiq.application.port.in.OverstockProductResult;
import com.inventoryiq.domain.model.ProductStatus;
import com.inventoryiq.domain.model.vo.CriticalityLevel;
import com.inventoryiq.domain.model.vo.ReorderPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica GenerateAlertsService con GetCriticalProductsUseCase y
 * DetectOverstockUseCase FAKEADOS (los puertos, no los repos ni las
 * implementaciones concretas): esto prueba únicamente la lógica de
 * composición/mapeo/filtrado de este caso de uso, sin re-probar el
 * cálculo interno de los otros dos (ya cubierto en sus propios tests).
 */
class GenerateAlertsServiceTest {

	private static final Long STORE_ID = 1L;
	private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-08-01");

	@Test
	void mapsCriticalProductsToStockoutAlertsWithSeverityFromCriticalityLevel() {
		FakeGetCriticalProductsUseCase criticalUseCase = new FakeGetCriticalProductsUseCase();
		criticalUseCase.results = List.of(
				criticalProduct(1001L, 80.0),  // HIGH (>=75)
				criticalProduct(1002L, 60.0),  // MEDIUM (>=50, <75)
				criticalProduct(1003L, 30.0)); // LOW (<50)

		var service = new GenerateAlertsService(criticalUseCase, new FakeDetectOverstockUseCase());

		List<AlertResult> alerts = service.execute(new GenerateAlertsQuery(STORE_ID, REFERENCE_DATE, null, null));

		assertEquals(3, alerts.size());
		assertTrue(alerts.stream().allMatch(a -> a.type() == AlertType.STOCKOUT));
		assertEquals(AlertSeverity.HIGH, severityOf(alerts, 1001L));
		assertEquals(AlertSeverity.MEDIUM, severityOf(alerts, 1002L));
		assertEquals(AlertSeverity.LOW, severityOf(alerts, 1003L));
	}

	@Test
	void mapsOverstockProductsToOverstockAlertsWithSeverityFromImmobilizedValue() {
		FakeDetectOverstockUseCase overstockUseCase = new FakeDetectOverstockUseCase();
		overstockUseCase.results = List.of(
				overstockProduct(2001L, new BigDecimal("600000")), // HIGH (>500000)
				overstockProduct(2002L, new BigDecimal("100000")), // MEDIUM (>50000, <=500000)
				overstockProduct(2003L, new BigDecimal("10000"))); // LOW (<=50000)

		var service = new GenerateAlertsService(new FakeGetCriticalProductsUseCase(), overstockUseCase);

		List<AlertResult> alerts = service.execute(new GenerateAlertsQuery(STORE_ID, REFERENCE_DATE, null, null));

		assertEquals(3, alerts.size());
		assertTrue(alerts.stream().allMatch(a -> a.type() == AlertType.OVERSTOCK));
		assertEquals(AlertSeverity.HIGH, severityOf(alerts, 2001L));
		assertEquals(AlertSeverity.MEDIUM, severityOf(alerts, 2002L));
		assertEquals(AlertSeverity.LOW, severityOf(alerts, 2003L));
	}

	@Test
	void filtersByType() {
		FakeGetCriticalProductsUseCase criticalUseCase = new FakeGetCriticalProductsUseCase();
		criticalUseCase.results = List.of(criticalProduct(1001L, 80.0));
		FakeDetectOverstockUseCase overstockUseCase = new FakeDetectOverstockUseCase();
		overstockUseCase.results = List.of(overstockProduct(2001L, new BigDecimal("600000")));

		var service = new GenerateAlertsService(criticalUseCase, overstockUseCase);

		List<AlertResult> stockoutOnly = service.execute(
				new GenerateAlertsQuery(STORE_ID, REFERENCE_DATE, AlertType.STOCKOUT, null));
		assertEquals(1, stockoutOnly.size());
		assertEquals(AlertType.STOCKOUT, stockoutOnly.get(0).type());

		List<AlertResult> overstockOnly = service.execute(
				new GenerateAlertsQuery(STORE_ID, REFERENCE_DATE, AlertType.OVERSTOCK, null));
		assertEquals(1, overstockOnly.size());
		assertEquals(AlertType.OVERSTOCK, overstockOnly.get(0).type());
	}

	@Test
	void filtersBySeverity() {
		FakeGetCriticalProductsUseCase criticalUseCase = new FakeGetCriticalProductsUseCase();
		criticalUseCase.results = List.of(criticalProduct(1001L, 80.0), criticalProduct(1002L, 30.0));

		var service = new GenerateAlertsService(criticalUseCase, new FakeDetectOverstockUseCase());

		List<AlertResult> highOnly = service.execute(
				new GenerateAlertsQuery(STORE_ID, REFERENCE_DATE, null, AlertSeverity.HIGH));

		assertEquals(1, highOnly.size());
		assertEquals(1001L, highOnly.get(0).productId());
	}

	@Test
	void setsGeneratedAtToTheQueryReferenceDate() {
		FakeGetCriticalProductsUseCase criticalUseCase = new FakeGetCriticalProductsUseCase();
		criticalUseCase.results = List.of(criticalProduct(1001L, 80.0));

		var service = new GenerateAlertsService(criticalUseCase, new FakeDetectOverstockUseCase());

		List<AlertResult> alerts = service.execute(new GenerateAlertsQuery(STORE_ID, REFERENCE_DATE, null, null));

		assertEquals(REFERENCE_DATE, alerts.get(0).generatedAt());
	}

	// ---- helpers ----

	private static AlertSeverity severityOf(List<AlertResult> alerts, Long productId) {
		return alerts.stream().filter(a -> a.productId().equals(productId)).findFirst().orElseThrow().severity();
	}

	private static CriticalProductResult criticalProduct(Long id, double score) {
		return new CriticalProductResult(id, "SKU-" + id, "Producto " + id, STORE_ID, 10L,
				5, new ReorderPoint(60.0), 3.0, ProductStatus.CRITICAL, new CriticalityLevel(score));
	}

	private static OverstockProductResult overstockProduct(Long id, BigDecimal immobilizedValue) {
		return new OverstockProductResult(id, "SKU-" + id, "Producto " + id, STORE_ID, 10L,
				100, 45.0, immobilizedValue);
	}

	private static class FakeGetCriticalProductsUseCase implements GetCriticalProductsUseCase {
		List<CriticalProductResult> results = List.of();

		@Override
		public List<CriticalProductResult> execute(GetCriticalProductsQuery query) {
			return results;
		}
	}

	private static class FakeDetectOverstockUseCase implements DetectOverstockUseCase {
		List<OverstockProductResult> results = List.of();

		@Override
		public List<OverstockProductResult> execute(DetectOverstockQuery query) {
			return results;
		}
	}
}

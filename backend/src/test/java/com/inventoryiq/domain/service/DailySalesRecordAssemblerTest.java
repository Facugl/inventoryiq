package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.vo.DailySalesRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailySalesRecordAssemblerTest {

	private static Sale sale(String date, int unitsSold) {
		return new Sale(1L, LocalDate.parse(date), 1001L, 1L, unitsSold, BigDecimal.valueOf(unitsSold * 100));
	}

	private static Inventory snapshot(String date, int currentStock) {
		return new Inventory(1L, LocalDate.parse(date), 1001L, 1L, currentStock, 0);
	}

	private static List<DailySalesRecord> assemble(List<Sale> sales, List<Inventory> snapshots, String from, String to) {
		return DailySalesRecordAssembler.assemble(sales, snapshots, LocalDate.parse(from), LocalDate.parse(to));
	}

	@Test
	void usesThePreviousDayClosingStockAsStartOfDayStock() {
		List<Sale> sales = List.of(sale("2025-07-04", 55));
		List<Inventory> snapshots = List.of(
				snapshot("2025-07-03", 314), // cierre del día anterior = inicio de 07-04
				snapshot("2025-07-04", 259));

		List<DailySalesRecord> result = assemble(sales, snapshots, "2025-07-04", "2025-07-04");

		assertEquals(1, result.size());
		assertEquals(55, result.get(0).unitsSold());
		assertEquals(314, result.get(0).stockAtStartOfDay());
		assertFalse(result.get(0).hadStockout());
	}

	@Test
	void excludesTheDayWhenThePreviousDaySnapshotIsMissing() {
		List<Sale> sales = List.of(
				sale("2025-07-03", 43), // primer día del dataset: no hay snapshot de 07-02
				sale("2025-07-04", 55));
		List<Inventory> snapshots = List.of(
				snapshot("2025-07-03", 314),
				snapshot("2025-07-04", 259));

		List<DailySalesRecord> result = assemble(sales, snapshots, "2025-07-03", "2025-07-04");

		assertEquals(1, result.size());
		assertEquals(LocalDate.parse("2025-07-04"), result.get(0).date());
	}

	@Test
	void detectsStockoutWhenThePreviousDayClosedAtZero() {
		List<Sale> sales = List.of(sale("2026-02-02", 0));
		List<Inventory> snapshots = List.of(
				snapshot("2026-02-01", 0),
				snapshot("2026-02-02", 0));

		List<DailySalesRecord> result = assemble(sales, snapshots, "2026-02-02", "2026-02-02");

		assertEquals(1, result.size());
		assertTrue(result.get(0).hadStockout());
	}

	/**
	 * Regresión: AdsCalculator divide por la cantidad de registros recibidos,
	 * así que un día sin ventas tiene que llegar igual, con cero unidades. Si
	 * se omitieran, el divisor sería "días en que se vendió" en vez de "días
	 * del período" y el ADS saldría inflado en proporción a lo poco que rote
	 * el producto — sin que nada falle.
	 */
	@Test
	void buildsOneRecordPerDayOfThePeriodEvenWithoutSales() {
		List<Sale> sales = List.of(sale("2025-07-05", 10)); // vendió un solo día de cuatro
		List<Inventory> snapshots = List.of(
				snapshot("2025-07-03", 100), snapshot("2025-07-04", 100),
				snapshot("2025-07-05", 90), snapshot("2025-07-06", 90),
				snapshot("2025-07-07", 90));

		List<DailySalesRecord> result = assemble(sales, snapshots, "2025-07-04", "2025-07-07");

		assertEquals(4, result.size());
		assertEquals(List.of(0, 10, 0, 0), result.stream().map(DailySalesRecord::unitsSold).toList());
		assertEquals(2.5, AdsCalculator.calculateCorrectedAds(result)); // 10/4, no 10/1
	}

	@Test
	void sumsSeveralSalesRecordedForTheSameDay() {
		List<Sale> sales = List.of(sale("2025-07-04", 20), sale("2025-07-04", 5));
		List<Inventory> snapshots = List.of(snapshot("2025-07-03", 100));

		List<DailySalesRecord> result = assemble(sales, snapshots, "2025-07-04", "2025-07-04");

		assertEquals(1, result.size());
		assertEquals(25, result.get(0).unitsSold());
	}

	@Test
	void returnsEmptyListForEmptyInputs() {
		assertTrue(assemble(List.of(), List.of(), "2025-07-01", "2025-07-31").isEmpty());
	}

	@Test
	void returnsEmptyListWhenThePeriodIsInverted() {
		List<Inventory> snapshots = List.of(snapshot("2025-07-03", 100));

		assertTrue(assemble(List.of(), snapshots, "2025-07-10", "2025-07-04").isEmpty());
	}
}

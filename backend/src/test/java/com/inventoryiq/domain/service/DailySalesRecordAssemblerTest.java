package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.vo.DailySalesRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailySalesRecordAssemblerTest {

	private static Sale sale(String date, int unitsSold) {
		return new Sale(1L, LocalDate.parse(date), 1001L, 1L, unitsSold, BigDecimal.valueOf(unitsSold * 100));
	}

	private static Inventory snapshot(String date, int currentStock) {
		return new Inventory(1L, LocalDate.parse(date), 1001L, 1L, currentStock, 0);
	}

	@Test
	void usesThePreviousDayClosingStockAsStartOfDayStock() {
		List<Sale> sales = List.of(sale("2025-07-04", 55));
		List<Inventory> snapshots = List.of(
				snapshot("2025-07-03", 314), // cierre del día anterior = inicio de 07-04
				snapshot("2025-07-04", 259));

		List<DailySalesRecord> result = DailySalesRecordAssembler.assemble(sales, snapshots);

		assertEquals(1, result.size());
		assertEquals(55, result.get(0).unitsSold());
		assertEquals(314, result.get(0).stockAtStartOfDay());
		assertTrue(!result.get(0).hadStockout());
	}

	@Test
	void excludesTheDayWhenThePreviousDaySnapshotIsMissing() {
		List<Sale> sales = List.of(
				sale("2025-07-03", 43), // primer día del dataset: no hay snapshot de 07-02
				sale("2025-07-04", 55));
		List<Inventory> snapshots = List.of(
				snapshot("2025-07-03", 314),
				snapshot("2025-07-04", 259));

		List<DailySalesRecord> result = DailySalesRecordAssembler.assemble(sales, snapshots);

		assertEquals(1, result.size());
		assertEquals(LocalDate.parse("2025-07-04"), result.get(0).date());
	}

	@Test
	void detectsStockoutWhenThePreviousDayClosedAtZero() {
		List<Sale> sales = List.of(sale("2026-02-02", 0));
		List<Inventory> snapshots = List.of(
				snapshot("2026-02-01", 0),
				snapshot("2026-02-02", 0));

		List<DailySalesRecord> result = DailySalesRecordAssembler.assemble(sales, snapshots);

		assertEquals(1, result.size());
		assertTrue(result.get(0).hadStockout());
	}

	@Test
	void returnsEmptyListForEmptyInputs() {
		assertTrue(DailySalesRecordAssembler.assemble(List.of(), List.of()).isEmpty());
	}
}

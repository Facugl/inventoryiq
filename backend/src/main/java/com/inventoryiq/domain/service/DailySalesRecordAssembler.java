package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.vo.DailySalesRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Combina ventas diarias con snapshots de inventario para construir la
 * serie que necesita AdsCalculator.calculateCorrectedAds (regla 4.9).
 *
 * Un snapshot de inventario representa el stock al CIERRE del día (se
 * verificó cruzando inventario.csv contra movimientos.csv: el
 * stock_actual de una fecha coincide con el stock_resultante del
 * movimiento de esa misma fecha, no con el stock antes de los
 * movimientos del día). Por lo tanto, el stock a INICIO del día D es el
 * stock de cierre del día D-1, no el snapshot del propio día D.
 *
 * Si no existe snapshot para el día anterior a una venta (borde del
 * historial disponible, por ejemplo el primer día de todo el dataset),
 * esa venta se excluye de la serie en vez de asumir un valor: es
 * preferible una ventana más corta a una ADS corregida artificialmente.
 */
public final class DailySalesRecordAssembler {
	private DailySalesRecordAssembler() {
	}

	public static List<DailySalesRecord> assemble(List<Sale> sales, List<Inventory> snapshots) {
		if (sales == null || snapshots == null) {
			return List.of();
		}

		Map<LocalDate, Integer> closingStockByDate = snapshots.stream()
				.collect(Collectors.toMap(Inventory::snapshotDate, Inventory::currentStock, (a, b) -> b));

		return sales.stream()
				.filter(sale -> closingStockByDate.containsKey(sale.date().minusDays(1)))
				.map(sale -> new DailySalesRecord(
						sale.date(),
						sale.unitsSold(),
						closingStockByDate.get(sale.date().minusDays(1))))
				.toList();
	}
}

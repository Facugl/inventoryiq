package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Sale;
import com.inventoryiq.domain.model.vo.DailySalesRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Combina ventas diarias con snapshots de inventario para construir la
 * serie que necesita AdsCalculator.calculateCorrectedAds (regla 4.9).
 *
 * Construye un registro por CADA día del período pedido, no solo por los
 * días que tienen una venta registrada. Es deliberado y es parte del
 * contrato con AdsCalculator: el ADS corregido divide por la cantidad de
 * registros que recibe, así que un día sin ventas tiene que llegar como
 * un día con cero unidades. Si se omitieran, el divisor sería "días en
 * que se vendió" en vez de "días del período", y el ADS saldría inflado
 * en proporción a lo poco que rote el producto — sin que nada falle.
 *
 * Un snapshot de inventario representa el stock al CIERRE del día (se
 * verificó cruzando inventario.csv contra movimientos.csv: el
 * stock_actual de una fecha coincide con el stock_resultante del
 * movimiento de esa misma fecha, no con el stock antes de los
 * movimientos del día). Por lo tanto, el stock a INICIO del día D es el
 * stock de cierre del día D-1, no el snapshot del propio día D.
 *
 * Si no existe snapshot para el día anterior a un día del período (borde
 * del historial disponible, por ejemplo el primer día de todo el
 * dataset), ese día se excluye de la serie en vez de asumir un valor: no
 * se puede saber si hubo quiebre, y suponerlo sesgaría el ADS en una
 * dirección u otra. Es preferible una ventana más corta a una ADS
 * corregida artificialmente.
 */
public final class DailySalesRecordAssembler {
	private DailySalesRecordAssembler() {
	}

	public static List<DailySalesRecord> assemble(
			List<Sale> sales, List<Inventory> snapshots, LocalDate from, LocalDate to) {

		if (sales == null || snapshots == null || from == null || to == null || from.isAfter(to)) {
			return List.of();
		}

		Map<LocalDate, Integer> closingStockByDate = snapshots.stream()
				.collect(Collectors.toMap(Inventory::snapshotDate, Inventory::currentStock, (a, b) -> b));

		// Se suman las unidades si llegara más de una venta para el mismo día:
		// el período es el día, así que dos registros son dos tramos del mismo.
		Map<LocalDate, Integer> unitsSoldByDate = sales.stream()
				.collect(Collectors.toMap(Sale::date, Sale::unitsSold, Integer::sum));

		List<DailySalesRecord> records = new ArrayList<>();
		for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
			Integer stockAtStartOfDay = closingStockByDate.get(day.minusDays(1));
			if (stockAtStartOfDay == null) {
				continue;
			}
			records.add(new DailySalesRecord(
					day, unitsSoldByDate.getOrDefault(day, 0), stockAtStartOfDay));
		}
		return records;
	}
}

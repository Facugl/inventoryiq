package com.inventoryiq.domain.model.vo;

import java.time.LocalDate;

/**
 * Representa un día de historial de ventas de un producto en una sucursal,
 * junto con el stock que había al inicio del día. Es el insumo para el
 * ADS corregido (Sección 4.9: censura de demanda en días de quiebre).
 */
public record DailySalesRecord(
		LocalDate date,
		int unitsSold,
		int stockAtStartOfDay) {

	public boolean hadStockout() {
		return stockAtStartOfDay <= 0;
	}
}

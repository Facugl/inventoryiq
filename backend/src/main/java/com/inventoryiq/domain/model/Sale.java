package com.inventoryiq.domain.model;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Sección 5.5 — Registro diario de unidades vendidas de un producto en una
 * sucursal (granularidad diaria agregada, no ticket por ticket).
 * Es el insumo, junto con Inventory, del ADS corregido (regla 4.9) vía
 * DailySalesRecordAssembler, y del valor de venta usado por AbcClassifier
 * (regla 4.7).
 */
public record Sale(
		Long saleId,
		LocalDate date,
		Long productId,
		Long storeId,
		int unitsSold,
		BigDecimal totalAmount) {

	public Sale {
		if (unitsSold < 0) {
			throw new InvalidDomainDataException("Units sold cannot be negative, received: " + unitsSold);
		}

		if (totalAmount == null || totalAmount.signum() < 0) {
			throw new InvalidDomainDataException("Total amount cannot be negative");
		}
	}
}

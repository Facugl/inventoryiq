package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Sección 4.6 — Rotación de Inventario. */
public final class InventoryTurnoverCalculator {
	private InventoryTurnoverCalculator() {
	}

	/**
	 * Rotación = Costo de Mercadería Vendida en el período / Inventario Promedio en
	 * el período.
	 */
	public static double calculate(BigDecimal costOfGoodsSold, BigDecimal averageInventory) {
		if (costOfGoodsSold == null || averageInventory == null) {
			throw new InvalidDomainDataException("The cost of goods sold and the average inventory are required");
		}

		if (averageInventory.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidDomainDataException("The average inventory must be greater than 0 to calculate the turnover");
		}
		
		return costOfGoodsSold.divide(averageInventory, 4, RoundingMode.HALF_UP).doubleValue();
	}
}
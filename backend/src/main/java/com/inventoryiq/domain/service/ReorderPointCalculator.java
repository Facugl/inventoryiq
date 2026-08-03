package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.vo.LeadTime;
import com.inventoryiq.domain.model.vo.ReorderPoint;
import com.inventoryiq.domain.model.vo.SafetyStock;

/** Sección 4.4 — Punto de Pedido (ROP) y regla de disparo de reposición. */
public final class ReorderPointCalculator {
	private ReorderPointCalculator() {
	}

	/** Punto de Pedido = (ADS × Lead Time) + Stock de Seguridad. */
	public static ReorderPoint calculate(double ads, LeadTime leadTime, SafetyStock safetyStock) {
		if (ads < 0) {
			throw new InvalidDomainDataException("ADS cannot be negative, received: " + ads);
		}

		double value = (ads * leadTime.days()) + safetyStock.units();
		
		return new ReorderPoint(value);
	}

	/**
	 * Regla de disparo: Stock Actual <= Punto de Pedido => "Requiere Reposición".
	 */
	public static boolean requiresReplenishment(int currentStock, ReorderPoint reorderPoint) {
		return currentStock <= reorderPoint.units();
	}
}
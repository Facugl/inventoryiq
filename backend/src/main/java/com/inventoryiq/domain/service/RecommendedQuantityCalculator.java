package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.vo.RecommendedQuantity;

/**
 * Sección 4.5 — Cantidad de Reposición Sugerida: cobertura objetivo y EOQ
 * alternativo.
 */
public final class RecommendedQuantityCalculator {
	private RecommendedQuantityCalculator() {
	}

	/**
	 * Cantidad Sugerida = (ADS × Días de Cobertura Objetivo) - Stock Actual - Stock
	 * en Tránsito.
	 * Se redondea a la unidad entera más cercana y nunca es negativa
	 * (si el stock actual + en tránsito ya cubre el objetivo, no se sugiere pedir
	 * nada).
	 */
	public static RecommendedQuantity calculateByTargetCoverage(
			double ads, int targetCoverageDays, int currentStock, int stockInTransit) {
		if (ads < 0) {
			throw new InvalidDomainDataException("ADS cannot be negative, received: " + ads);
		}

		if (targetCoverageDays <= 0) {
			throw new InvalidDomainDataException(
					"Target coverage days must be greater than 0, received: " + targetCoverageDays);
		}

		double suggested = (ads * targetCoverageDays) - currentStock - stockInTransit;

		int rounded = (int) Math.max(0, Math.round(suggested));

		return new RecommendedQuantity(rounded);
	}

	/**
	 * EOQ = √( (2 × Demanda Anual × Costo de Pedido) / Costo de Mantenimiento por
	 * Unidad ).
	 */
	public static RecommendedQuantity calculateEoq(double annualDemand, double orderCost,
			double holdingCostPerUnit) {
		if (annualDemand <= 0 || orderCost <= 0) {
			throw new InvalidDomainDataException(
					"Annual demand and order cost must be greater than 0 to calculate EOQ");
		}

		if (holdingCostPerUnit <= 0) {
			throw new InvalidDomainDataException(
					"Holding cost per unit must be greater than 0 to calculate EOQ");
		}

		double eoq = Math.sqrt((2 * annualDemand * orderCost) / holdingCostPerUnit);
		
		return new RecommendedQuantity((int) Math.round(eoq));
	}
}
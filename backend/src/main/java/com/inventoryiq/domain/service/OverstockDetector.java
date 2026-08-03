package com.inventoryiq.domain.service;

/** Sección 4.8 — Detección de Sobrestock por cobertura excesiva. */
public final class OverstockDetector {
	private OverstockDetector() {
	}

	/**
	 * Días de Cobertura Actual = Stock Actual / ADS.
	 * Si ADS es 0 (sin ventas registradas), la cobertura es infinita:
	 * cualquier stock > 0 sin demanda es, por definición, sobrestock.
	 */
	public static double calculateCurrentDaysOfCoverage(int currentStock, double ads) {
		if (ads <= 0) {
			return currentStock > 0 ? Double.POSITIVE_INFINITY : 0.0;
		}

		return currentStock / ads;
	}

	/**
	 * Sobrestock si: Días de Cobertura Actual > Umbral Máximo de Cobertura
	 * (parámetro por categoría).
	 */
	public static boolean isOverstock(int currentStock, double ads, int maximumCoverageThresholdDays) {
		double days = calculateCurrentDaysOfCoverage(currentStock, ads);
		
		return days > maximumCoverageThresholdDays;
	}
}
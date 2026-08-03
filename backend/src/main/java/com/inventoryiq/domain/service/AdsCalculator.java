package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.vo.DailySalesRecord;

import java.util.List;

/**
 * Sección 4.1 y 4.9 — Venta promedio diaria (ADS), simple y corregida por
 * quiebres.
 */
public final class AdsCalculator {
	private AdsCalculator() {
	}

	/** 4.1 — ADS simple: todos los días pesan igual, sin corregir por quiebres. */
	public static double calculateSimpleAds(List<Integer> unitsSoldPerDay) {
		if (unitsSoldPerDay == null || unitsSoldPerDay.isEmpty()) {
			throw new InvalidDomainDataException("At least one day of sales is needed to calculate ADS");
		}

		double total = unitsSoldPerDay.stream().mapToInt(Integer::intValue).sum();
		return total / unitsSoldPerDay.size();
	}

	/**
	 * 4.9 — ADS corregido: excluye del cálculo los días en que el producto
	 * estuvo en quiebre de stock (stock=0 al inicio del día), porque esos
	 * días no reflejan demanda real, sino demanda no satisfecha.
	 */
	public static double calculateCorrectedAds(List<DailySalesRecord> records) {
		if (records == null || records.isEmpty()) {
			throw new InvalidDomainDataException("At least one sales record is needed to calculate corrected ADS");
		}

		List<DailySalesRecord> daysWithoutStockout = records.stream()
				.filter(r -> !r.hadStockout())
				.toList();

		if (daysWithoutStockout.isEmpty()) {
			throw new InvalidDomainDataException(
					"All days in the period had stockout; cannot estimate corrected ADS");
		}

		double totalSold = daysWithoutStockout.stream().mapToInt(DailySalesRecord::unitsSold).sum();
		return totalSold / daysWithoutStockout.size();
	}
}
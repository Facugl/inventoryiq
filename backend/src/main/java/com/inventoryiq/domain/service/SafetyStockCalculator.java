package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.vo.LeadTime;
import com.inventoryiq.domain.model.vo.SafetyStock;

/**
 * Sección 4.3 — Stock de Seguridad, método estadístico y método simplificado
 * (MVP).
 */
public final class SafetyStockCalculator {
	private SafetyStockCalculator() {
	}

	/** Stock de Seguridad = Z × σ_demanda × √(Lead Time). */
	public static SafetyStock calculateStatisticalMethod(double serviceFactorZ, double demandStandardDeviation,
			LeadTime leadTime) {
		if (serviceFactorZ <= 0) {
			throw new InvalidDomainDataException(
					"Service factor Z must be greater than 0, received: " + serviceFactorZ);
		}

		if (demandStandardDeviation < 0) {
			throw new InvalidDomainDataException(
					"Demand standard deviation cannot be negative, received: " + demandStandardDeviation);
		}

		double value = serviceFactorZ * demandStandardDeviation * Math.sqrt(leadTime.days());

		return new SafetyStock(value);
	}

	/**
	 * Stock de Seguridad = ADS × Días de Cobertura Extra (parámetro configurable
	 * por categoría).
	 */
	public static SafetyStock calculateSimplifiedMethod(double ads, int extraCoverageDays) {
		if (ads < 0) {
			throw new InvalidDomainDataException("ADS cannot be negative, received: " + ads);
		}

		if (extraCoverageDays < 0) {
			throw new InvalidDomainDataException(
					"Extra coverage days cannot be negative, received: " + extraCoverageDays);
		}
		
		return new SafetyStock(ads * extraCoverageDays);
	}
}
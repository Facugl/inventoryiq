package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.util.List;

/**
 * Utilidades estadísticas puras (media, desvío estándar, coeficiente de
 * variación) usadas por SafetyStockCalculator (método estadístico)
 * y por XyZClassifier. Se calcula el desvío estándar POBLACIONAL
 * (se divide por n, no por n-1): en este dominio siempre trabajamos con
 * el historial completo disponible como "la población" de referencia,
 * no con una muestra de un universo más grande.
 */
public final class DemandStatistics {
	private DemandStatistics() {
	}

	public static double mean(List<Integer> values) {
		validateNotEmpty(values);

		return values.stream().mapToInt(Integer::intValue).average().orElseThrow();
	}

	public static double standardDeviation(List<Integer> values) {
		validateNotEmpty(values);

		double mean = mean(values);
		double sumOfSquares = values.stream()
				.mapToDouble(v -> Math.pow(v - mean, 2))
				.sum();

		return Math.sqrt(sumOfSquares / values.size());
	}

	/**
	 * CV = σ / ADS (Sección 4.7). Si la media es 0, se define CV = 0 (sin demanda,
	 * sin variabilidad que medir).
	 */
	public static double coefficientOfVariation(List<Integer> values) {
		double mean = mean(values);

		if (mean == 0) {
			return 0.0;
		}
		
		return standardDeviation(values) / mean;
	}

	private static void validateNotEmpty(List<Integer> values) {
		if (values == null || values.isEmpty()) {
			throw new InvalidDomainDataException("At least one value is needed to calculate demand statistics");
		}
	}
}
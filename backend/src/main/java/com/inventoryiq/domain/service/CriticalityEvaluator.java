package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.AbcClassification;
import com.inventoryiq.domain.model.vo.LeadTime;
import com.inventoryiq.domain.model.vo.CriticalityLevel;

import java.util.Map;

/** Sección 4.11 — Score de criticidad (0 a 100) para priorizar alertas. */
public final class CriticalityEvaluator {
	private CriticalityEvaluator() {
	}

	/**
	 * Valor numérico asociado a cada clase ABC para el término Valor_ABC de la
	 * fórmula. El documento no fija estos valores; se documentan acá como
	 * convención del dominio (A pesa más que B, que pesa más que C).
	 */
	private static final Map<AbcClassification, Double> ABC_VALUE = Map.of(
			AbcClassification.A, 1.0,
			AbcClassification.B, 0.6,
			AbcClassification.C, 0.3);

	public record CriticalityWeights(double abcWeight, double coverageWeight, double stockoutWeight) {
		public CriticalityWeights {
			if (abcWeight < 0 || coverageWeight < 0 || stockoutWeight < 0) {
				throw new InvalidDomainDataException("Criticality weights cannot be negative");
			}
		}
	}

	/**
	 * Score = (Peso_ABC × Valor_ABC) + (Peso_Cobertura × (1 -
	 * Días_Cobertura_Restante / Lead_Time))
	 * + (Peso_Quiebre × Indicador_Ya_Quebrado)
	 * El resultado se expresa en escala 0-100 y se recorta a ese rango
	 * (si los pesos no suman exactamente 1, el score igual queda acotado).
	 */
	public static CriticalityLevel calculate(
			AbcClassification abcClass,
			double remainingCoverageDays,
			LeadTime leadTime,
			boolean alreadyStockout,
			CriticalityWeights weights) {
		double abcValue = ABC_VALUE.get(abcClass);

		double coverageFactor = 1 - (remainingCoverageDays / leadTime.days());
		coverageFactor = Math.max(0, Math.min(1, coverageFactor)); // clamp to [0,1]

		double stockoutIndicator = alreadyStockout ? 1 : 0;

		double score = (weights.abcWeight() * abcValue * 100)
				+ (weights.coverageWeight() * coverageFactor * 100)
				+ (weights.stockoutWeight() * stockoutIndicator * 100);

		score = Math.max(0, Math.min(100, score));
		return new CriticalityLevel(score);
	}
}
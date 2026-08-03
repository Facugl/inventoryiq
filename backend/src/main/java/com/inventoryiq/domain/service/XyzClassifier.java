package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.XyzClassification;

/**
 * Sección 4.7 — Clasificación XYZ según coeficiente de variación de la demanda.
 */
public final class XyzClassifier {
	private XyzClassifier() {
	}

	/** CV = σ_demanda / ADS. X: CV < 0.5 | Y: 0.5 <= CV < 1.0 | Z: CV >= 1.0. */
	public static XyzClassification classify(double ads, double demandStandardDeviation) {
		if (ads <= 0) {
			throw new InvalidDomainDataException("ADS must be greater than 0 to calculate the coefficient of variation");
		}

		if (demandStandardDeviation < 0) {
			throw new InvalidDomainDataException("Demand standard deviation cannot be negative");
		}

		double cv = demandStandardDeviation / ads;

		if (cv < 0.5) {
			return XyzClassification.X;
		}

		if (cv < 1.0) {
			return XyzClassification.Y;
		}
		
		return XyzClassification.Z;
	}
}
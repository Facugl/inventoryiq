package com.inventoryiq.domain.model.vo;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

/**
 * Sección 4.11 — Score de criticidad (0 a 100) usado para priorizar alertas.
 * Los umbrales de isCritical()/isHigh() son una convención del dominio,
 * no están fijados por el documento; se documentan acá para que quede
 * centralizado si hay que ajustarlos.
 */
public record CriticalityLevel(double score) {
	private static final double CRITICAL_THRESHOLD = 75.0;
	private static final double HIGH_THRESHOLD = 50.0;

	public CriticalityLevel {
		if (score < 0 || score > 100) {
			throw new InvalidDomainDataException("The criticality score must be between 0 and 100, received: " + score);
		}
	}

	public boolean isCritical() {
		return score >= CRITICAL_THRESHOLD;
	}

	public boolean isHigh() {
		return score >= HIGH_THRESHOLD;
	}
}

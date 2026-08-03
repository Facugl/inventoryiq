package com.inventoryiq.domain.model.vo;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

/**
 * Sección 4.3 — Colchón de stock para absorber variabilidad de demanda o de
 * lead time.
 */
public record SafetyStock(double units) {
	public SafetyStock {
		if (units < 0) {
			throw new InvalidDomainDataException("The safety stock cannot be negative, received: " + units);
		}
	}
}

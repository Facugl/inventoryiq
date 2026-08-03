package com.inventoryiq.domain.model.vo;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

/**
 * Sección 4.4 — Nivel de stock en el cual se debe disparar una nueva orden de
 * compra.
 */
public record ReorderPoint(double units) {
	public ReorderPoint {
		if (units < 0) {
			throw new InvalidDomainDataException("The reorder point cannot be negative, received: " + units);
		}
	}
}

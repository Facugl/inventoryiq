package com.inventoryiq.domain.model.vo;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

/**
 * Sección 4.5 — Cantidad de unidades a pedir cuando se dispara el punto de
 * pedido.
 * Siempre en unidades enteras: no se piden fracciones de producto.
 */
public record RecommendedQuantity(int units) {
	public RecommendedQuantity {
		if (units < 0) {
			throw new InvalidDomainDataException("The recommended quantity cannot be negative, received: " + units);
		}
	}
}

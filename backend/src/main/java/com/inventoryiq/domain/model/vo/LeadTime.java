package com.inventoryiq.domain.model.vo;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

/**
 * Sección 4.2 — Tiempo de reposición, en días, desde que se emite una orden
 * de compra hasta que la mercadería está disponible para la venta.
 */
public record LeadTime(int days) {
	public LeadTime {
		if (days <= 0) {
			throw new InvalidDomainDataException("Lead time must be greater than 0 days, received: " + days);
		}
	}
}

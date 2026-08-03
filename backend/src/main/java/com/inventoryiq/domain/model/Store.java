package com.inventoryiq.domain.model;

/** Sección 5.4 — Punto de venta físico. */
public record Store(
		Long storeId,
		String name,
		String address,
		boolean active) {
}

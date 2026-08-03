package com.inventoryiq.domain.model;

/**
 * Sección 5.8 — Tipos de movimiento de stock registrados en movimientos.csv.
 */
public enum MovementType {
	PURCHASE_INBOUND,
	SALE_OUTBOUND,
	POSITIVE_ADJUSTMENT,
	NEGATIVE_ADJUSTMENT
}

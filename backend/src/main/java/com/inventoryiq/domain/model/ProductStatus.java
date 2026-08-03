package com.inventoryiq.domain.model;

/**
 * Sección 4.12 — Máquina de estados de un producto en una sucursal.
 * El orden de los valores no implica prioridad de evaluación; esa lógica
 * vive en ProductStatusEvaluator.
 */
public enum ProductStatus {
	NORMAL,
	REQUIRES_REPLENISHMENT,
	CRITICAL,
	OVERSTOCK,
	LOW_ROTATION
}

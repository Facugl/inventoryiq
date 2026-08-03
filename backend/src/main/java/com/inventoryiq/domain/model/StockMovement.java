package com.inventoryiq.domain.model;

import java.time.LocalDate;

/** Sección 5.8 — Registro trazable de entrada o salida de stock. */
public record StockMovement(
		Long movementId,
		LocalDate date,
		Long productId,
		Long storeId,
		MovementType movementType,
		int quantity,
		int resultingStock) {
}

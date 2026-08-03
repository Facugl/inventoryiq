package com.inventoryiq.domain.model;

import java.time.LocalDate;

/** Sección 5.7 — Snapshot diario de stock de un producto en una sucursal. */
public record Inventory(
		Long inventoryId,
		LocalDate snapshotDate,
		Long productId,
		Long storeId,
		int currentStock,
		int stockInTransit) {
}

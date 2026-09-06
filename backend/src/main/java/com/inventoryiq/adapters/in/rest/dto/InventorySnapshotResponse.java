package com.inventoryiq.adapters.in.rest.dto;

import java.time.LocalDate;

/** Forma JSON pública de un snapshot de inventario (POST /api/v1/inventory-snapshots). */
public record InventorySnapshotResponse(
		Long inventoryId,
		LocalDate snapshotDate,
		Long productId,
		Long storeId,
		int currentStock,
		int stockInTransit) {
}

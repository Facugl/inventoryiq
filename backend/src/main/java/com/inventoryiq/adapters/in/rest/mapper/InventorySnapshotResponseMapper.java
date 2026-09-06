package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.InventorySnapshotResponse;
import com.inventoryiq.application.port.in.InventorySnapshotResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class InventorySnapshotResponseMapper {
	private InventorySnapshotResponseMapper() {
	}

	public static InventorySnapshotResponse toResponse(InventorySnapshotResult result) {
		return new InventorySnapshotResponse(
				result.inventoryId(), result.snapshotDate(), result.productId(), result.storeId(),
				result.currentStock(), result.stockInTransit());
	}
}

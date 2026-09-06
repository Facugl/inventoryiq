package com.inventoryiq.application.port.in;

import java.time.LocalDate;

/** Salida de RecordInventoryCountUseCase — el snapshot tal como quedó persistido. */
public record InventorySnapshotResult(
		Long inventoryId,
		LocalDate snapshotDate,
		Long productId,
		Long storeId,
		int currentStock,
		int stockInTransit) {
}

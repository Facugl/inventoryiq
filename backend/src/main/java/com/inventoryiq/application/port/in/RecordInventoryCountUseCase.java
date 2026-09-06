package com.inventoryiq.application.port.in;

/** Puerto de entrada. Registra un conteo físico manual de stock como un nuevo snapshot de inventario. */
public interface RecordInventoryCountUseCase {

	InventorySnapshotResult execute(RecordInventoryCountCommand command);
}

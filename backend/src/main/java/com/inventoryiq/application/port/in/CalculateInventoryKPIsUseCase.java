package com.inventoryiq.application.port.in;

/** Puerto de entrada — Sección 8.8/9.7. */
public interface CalculateInventoryKPIsUseCase {

	InventoryKPIsResult execute(CalculateInventoryKPIsQuery query);
}

package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Inventory;

import java.time.LocalDate;

/**
 * Puerto de salida — registra un nuevo snapshot de inventario (por ejemplo,
 * un conteo físico manual antes de pedirle a un proveedor). Separado de
 * InventoryRepository (lectura) siguiendo el mismo criterio que
 * SaleRepository/SaleIngestionRepository.
 *
 * inventoryId no lo elige quien llama: a diferencia de una venta ingerida
 * desde un CSV (que ya trae su propio id), un conteo manual no tiene un id
 * externo — lo asigna el adaptador de persistencia.
 */
public interface InventoryIngestionRepository {

	Inventory save(LocalDate snapshotDate, Long productId, Long storeId, int currentStock, int stockInTransit);
}

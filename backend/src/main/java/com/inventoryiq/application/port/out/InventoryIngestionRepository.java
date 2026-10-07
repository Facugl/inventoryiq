package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Inventory;

import java.time.LocalDate;

/**
 * Puerto de salida — registra un nuevo snapshot de inventario, sea un
 * conteo físico manual (RecordInventoryCountUseCase) o una fila de un
 * CSV de inventario ingerido (IngestCsvFileUseCase con
 * CsvFileType.INVENTORY). Separado de InventoryRepository (lectura)
 * siguiendo el mismo criterio que SaleRepository/SaleIngestionRepository.
 *
 * inventoryId no lo elige quien llama: a diferencia de una venta ingerida
 * desde un CSV (que ya trae su propio id), ni un conteo manual ni una
 * fila de inventario subida tienen un id externo utilizable — lo asigna
 * el adaptador de persistencia.
 */
public interface InventoryIngestionRepository {

	/** Mismo producto + sucursal + fecha de snapshot ya cargado, candidato a duplicado (Sección 7.3). */
	boolean existsByProductStoreAndSnapshotDate(Long productId, Long storeId, LocalDate snapshotDate);

	Inventory save(LocalDate snapshotDate, Long productId, Long storeId, int currentStock, int stockInTransit);
}

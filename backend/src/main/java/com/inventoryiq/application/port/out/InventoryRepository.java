package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Inventory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Puerto de salida — Sección 2.3.2. Rangos de fechas inclusivos en ambos extremos. */
public interface InventoryRepository {

	/** El snapshot más reciente con fecha <= asOfDate (tolera huecos puntuales de datos). */
	Optional<Inventory> findLatestSnapshotAsOf(Long productId, Long storeId, LocalDate asOfDate);

	List<Inventory> findSnapshotsInRange(Long productId, Long storeId, LocalDate from, LocalDate to);
}

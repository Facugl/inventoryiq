package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Store;

import java.util.List;
import java.util.Optional;

/** Puerto de salida — Sección 2.3.2. Abstrae el origen de datos de sucursales (CSV hoy, Postgres mañana). */
public interface StoreRepository {

	Optional<Store> findById(Long storeId);

	List<Store> findAllActive();
}

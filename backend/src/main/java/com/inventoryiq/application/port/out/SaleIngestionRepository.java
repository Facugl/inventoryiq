package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Sale;

import java.time.LocalDate;

/**
 * Puerto de salida — Sección 9.9. Separado de SaleRepository (solo
 * lectura) a propósito: es el primer puerto de escritura sobre datos
 * respaldados por CSV de este proyecto, y así ningún caso de uso ni test
 * existente que depende de SaleRepository necesita conocerlo.
 */
public interface SaleIngestionRepository {

	/** Sección 7.3 — mismo producto + sucursal + fecha ya cargado, candidato a duplicado. */
	boolean existsByProductStoreAndDate(Long productId, Long storeId, LocalDate date);

	void save(Sale sale);
}

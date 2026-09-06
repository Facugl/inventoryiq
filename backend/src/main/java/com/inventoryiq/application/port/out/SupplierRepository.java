package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Supplier;
import com.inventoryiq.domain.model.vo.LeadTime;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida — Sección 5.3. A diferencia de SaleRepository/InventoryRepository
 * (registros históricos inmutables, con un puerto de ingestión separado para
 * agregar filas), un proveedor es una entidad mutable: su lead time se
 * corrige con el tiempo a medida que el usuario lo conoce mejor, no se
 * versiona. Por eso, igual que RecommendationRepository, un único puerto
 * cubre lectura y actualización.
 */
public interface SupplierRepository {

	Optional<Supplier> findById(Long supplierId);

	List<Supplier> findAllActive();

	/** Actualiza el lead time promedio de un proveedor existente y devuelve el proveedor resultante. */
	Supplier updateLeadTime(Long supplierId, LeadTime newLeadTime);
}

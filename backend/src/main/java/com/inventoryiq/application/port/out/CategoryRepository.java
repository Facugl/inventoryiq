package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Category;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida — Sección 2.3.2. Igual que SupplierRepository, un único
 * puerto cubre lectura y actualización: una categoría es una entidad
 * mutable cuyos parámetros de negocio se corrigen con el tiempo, no un
 * registro histórico que se acumula.
 */
public interface CategoryRepository {

	Optional<Category> findById(Long categoryId);

	List<Category> findAll();

	/** Actualiza los parámetros de negocio de una categoría existente y devuelve la categoría resultante. */
	Category updateParameters(Long categoryId, int maxCoverageDaysThreshold, int defaultExtraCoverageDays);
}

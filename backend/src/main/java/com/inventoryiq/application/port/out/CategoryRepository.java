package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Category;

import java.util.Optional;

/** Puerto de salida — Sección 2.3.2. */
public interface CategoryRepository {

	Optional<Category> findById(Long categoryId);
}

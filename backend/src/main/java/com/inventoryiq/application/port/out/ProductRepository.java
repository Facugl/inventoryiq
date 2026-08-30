package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Product;

import java.util.List;
import java.util.Optional;

/** Puerto de salida — Sección 2.3.2. Abstrae el origen de datos de productos (CSV hoy, Postgres mañana). */
public interface ProductRepository {

	List<Product> findAllActive();

	Optional<Product> findById(Long productId);
}

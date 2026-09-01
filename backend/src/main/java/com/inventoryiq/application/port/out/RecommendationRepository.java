package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Recommendation;
import com.inventoryiq.domain.model.RecommendationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida — Secciones 8.5/8.6/8.7. Primer puerto de este proyecto
 * respaldado por Postgres en vez de CSV (Sección 2.3.2 sigue aplicando: el
 * dominio y la aplicación no saben cuál es el origen de datos concreto).
 */
public interface RecommendationRepository {

	/** Inserta si recommendationId es null (y le asigna un id nuevo); actualiza si no. */
	Recommendation save(Recommendation recommendation);

	Optional<Recommendation> findById(Long recommendationId);

	/** supplierId y status son opcionales (null = sin filtrar por ese criterio). */
	List<Recommendation> findByFilters(Long storeId, Long supplierId, RecommendationStatus status);
}

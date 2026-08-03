package com.inventoryiq.domain.model;

/**
 * Sección 5.2 — Categoría de productos, con sus parámetros de cobertura
 * (usados por el Stock de Seguridad y la Detección de Sobrestock).
 * parentCategoryId es null para categorías raíz.
 */
public record Category(
		Long categoryId,
		String name,
		Long parentCategoryId,
		int maxCoverageDaysThreshold,
		int defaultExtraCoverageDays) {
}

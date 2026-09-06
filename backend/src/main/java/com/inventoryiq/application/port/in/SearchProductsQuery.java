package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

/**
 * Parámetros de entrada de SearchProductsUseCase. Sin endpoint documentado
 * en la Sección 8 — se agrega para que un selector de producto (por
 * ejemplo, un conteo manual de stock) pueda buscar por código (código de
 * barras u otro código interno, ambos viven en Product.sku) o por nombre,
 * en vez de requerir que quien carga el dato memorice un productId numérico.
 */
public record SearchProductsQuery(String searchTerm) {

	public SearchProductsQuery {
		if (searchTerm == null || searchTerm.isBlank()) {
			throw new InvalidDomainDataException("searchTerm is required");
		}
	}
}

package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.RecommendationStatus;

/**
 * Parámetros de entrada de ListRecommendationsUseCase (Sección 8.5).
 *
 * storeId obligatorio, mismo criterio que el resto del proyecto
 * (single-store scope, sin StoreRepository). categoryId no existe como
 * columna en la tabla recommendations (solo conoce productId/storeId/
 * supplierId): el filtro se resuelve en el servicio de aplicación
 * consultando ProductRepository, igual que GenerateReorderSuggestions
 * filtra por categoría en memoria.
 */
public record ListRecommendationsQuery(
		Long storeId,
		Long categoryId,
		Long supplierId,
		RecommendationStatus status) {

	public ListRecommendationsQuery {
		if (storeId == null) {
			throw new InvalidDomainDataException(
					"storeId is required in this vertical slice (single-store scope, see class Javadoc)");
		}
	}
}

package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de GenerateReorderSuggestionsUseCase (Sección 8.5/9.2).
 *
 * storeId y referenceDate obligatorios por el mismo motivo que en el
 * resto del proyecto (single-store scope, sin StoreRepository; el
 * dominio nunca resuelve "hoy" internamente).
 *
 * Sin "estado" (pendiente/aplicada/descartada): la Sección 8.5 lo lista
 * como filtro, pero implica persistir recomendaciones con un ciclo de
 * vida — eso es el trabajo de RegisterRecommendationFeedback (9.8),
 * todavía no implementado. Este caso de uso calcula recomendaciones al
 * vuelo, sin persistirlas, igual que los otros cuatro de este proyecto.
 */
public record GenerateReorderSuggestionsQuery(
		Long storeId,
		Long categoryId,
		Long supplierId,
		LocalDate referenceDate) {

	public GenerateReorderSuggestionsQuery {
		if (storeId == null) {
			throw new InvalidDomainDataException(
					"storeId is required in this vertical slice (single-store scope, see class Javadoc)");
		}

		if (referenceDate == null) {
			throw new InvalidDomainDataException(
					"referenceDate is required; the use case never reads the system clock");
		}
	}
}

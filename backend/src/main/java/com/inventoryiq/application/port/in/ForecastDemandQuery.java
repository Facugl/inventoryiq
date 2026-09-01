package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de ForecastDemandUseCase (Sección 9.4).
 *
 * A diferencia de los otros cinco casos de uso de este proyecto, productId
 * es obligatorio: este caso de uso proyecta la demanda de UN producto
 * puntual, no de un catálogo completo. storeId y referenceDate siguen el
 * mismo criterio que el resto (single-store scope, el dominio nunca lee
 * el reloj del sistema). horizonDays es la entrada explícita que lista la
 * propia Sección 9.4 ("horizonte temporal (días)").
 */
public record ForecastDemandQuery(
		Long productId,
		Long storeId,
		LocalDate referenceDate,
		int horizonDays) {

	public ForecastDemandQuery {
		if (productId == null) {
			throw new InvalidDomainDataException("productId is required");
		}

		if (storeId == null) {
			throw new InvalidDomainDataException(
					"storeId is required in this vertical slice (single-store scope, see class Javadoc)");
		}

		if (referenceDate == null) {
			throw new InvalidDomainDataException("referenceDate is required; the use case never reads the system clock");
		}

		if (horizonDays <= 0) {
			throw new InvalidDomainDataException("horizonDays must be greater than 0, received: " + horizonDays);
		}
	}
}

package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de CalculateInventoryKPIsUseCase (Sección 8.8/9.7).
 *
 * storeId obligatorio, mismo criterio que el resto del proyecto
 * (single-store scope, sin StoreRepository). Los KPIs "de foto" (tasa de
 * quiebre, cobertura promedio, capital inmovilizado) se evalúan a
 * toDate; los "de período" (rotación, % de recomendaciones seguidas)
 * usan el rango [fromDate, toDate] completo.
 */
public record CalculateInventoryKPIsQuery(Long storeId, LocalDate fromDate, LocalDate toDate) {

	public CalculateInventoryKPIsQuery {
		if (storeId == null) {
			throw new InvalidDomainDataException(
					"storeId is required in this vertical slice (single-store scope, see class Javadoc)");
		}

		if (fromDate == null || toDate == null) {
			throw new InvalidDomainDataException("fromDate and toDate are required; the use case never reads the system clock");
		}

		if (fromDate.isAfter(toDate)) {
			throw new InvalidDomainDataException("fromDate cannot be after toDate");
		}
	}
}

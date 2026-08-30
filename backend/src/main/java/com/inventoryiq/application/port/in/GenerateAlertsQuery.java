package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de GenerateAlertsUseCase (Sección 8.14/9.6).
 *
 * storeId y referenceDate obligatorios por el mismo motivo que en los
 * otros casos de uso de este proyecto (single-store scope, sin
 * StoreRepository; el dominio nunca resuelve "hoy" internamente). La
 * Sección 9.6 describe "sucursal" como opcional a nivel de negocio, pero
 * mantenemos la misma restricción ya arrastrada por todo el proyecto.
 *
 * type y severity son filtros opcionales (Sección 8.14): si se omiten,
 * se devuelven todas las alertas de cualquier tipo/severidad.
 */
public record GenerateAlertsQuery(
		Long storeId,
		LocalDate referenceDate,
		AlertType type,
		AlertSeverity severity) {

	public GenerateAlertsQuery {
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

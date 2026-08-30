package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de DetectOverstockUseCase (Sección 8.4).
 *
 * storeId y referenceDate son obligatorios por el mismo motivo que en
 * GetCriticalProductsQuery: storeId porque este slice todavía no soporta
 * "todas las sucursales" (no existe StoreRepository), y referenceDate
 * porque el dominio nunca resuelve "hoy" internamente.
 *
 * A diferencia de GetCriticalProductsQuery, no tiene "limit": la Sección
 * 8.4 no lo pide como parámetro para este endpoint. Sí tiene sortBy,
 * porque acá "valor inmovilizado" y "días de cobertura" son dos preguntas
 * de negocio genuinamente distintas (no hay una métrica compuesta que las
 * combine sin perder información, a diferencia del score de criticidad).
 */
public record DetectOverstockQuery(
		Long storeId,
		Long categoryId,
		LocalDate referenceDate,
		int salesWindowDays,
		OverstockSortBy sortBy) {

	public DetectOverstockQuery {
		if (storeId == null) {
			throw new InvalidDomainDataException(
					"storeId is required in this vertical slice (single-store scope, see class Javadoc)");
		}

		if (referenceDate == null) {
			throw new InvalidDomainDataException(
					"referenceDate is required; the use case never reads the system clock");
		}

		if (salesWindowDays <= 0) {
			throw new InvalidDomainDataException(
					"salesWindowDays must be greater than 0, received: " + salesWindowDays);
		}

		if (sortBy == null) {
			throw new InvalidDomainDataException("sortBy is required");
		}
	}

	/** Ventana de ventas por defecto para ADS: 90 días (Sección 4.1), igual que GetCriticalProductsQuery. */
	public static DetectOverstockQuery of(Long storeId, Long categoryId, LocalDate referenceDate, OverstockSortBy sortBy) {
		return new DetectOverstockQuery(storeId, categoryId, referenceDate, 90, sortBy);
	}
}

package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de ClassifyProductsUseCase (Sección 9.5).
 *
 * storeId y referenceDate son obligatorios por el mismo motivo que en los
 * otros dos casos de uso de este slice (single-store scope, sin
 * StoreRepository; el dominio nunca resuelve "hoy" internamente).
 *
 * A diferencia de GetCriticalProductsQuery/DetectOverstockQuery (ventana
 * de 90 días, pensada para una señal operativa de corto plazo), acá la
 * ventana de análisis es de 365 días: la Sección 9.5 sugiere "últimos 12
 * meses" porque una clasificación ABC/XYZ describe el comportamiento de
 * un producto a largo plazo, no algo que deba recalcularse con la
 * volatilidad de una semana. No se expone como parámetro por la misma
 * razón que salesWindowDays no se expone en los otros dos endpoints.
 */
public record ClassifyProductsQuery(
		Long storeId,
		Long categoryId,
		LocalDate referenceDate,
		int analysisWindowDays) {

	public ClassifyProductsQuery {
		if (storeId == null) {
			throw new InvalidDomainDataException(
					"storeId is required in this vertical slice (single-store scope, see class Javadoc)");
		}

		if (referenceDate == null) {
			throw new InvalidDomainDataException(
					"referenceDate is required; the use case never reads the system clock");
		}

		if (analysisWindowDays <= 0) {
			throw new InvalidDomainDataException(
					"analysisWindowDays must be greater than 0, received: " + analysisWindowDays);
		}
	}

	/** Ventana de análisis por defecto: 365 días (Sección 9.5, "últimos 12 meses"). */
	public static ClassifyProductsQuery of(Long storeId, Long categoryId, LocalDate referenceDate) {
		return new ClassifyProductsQuery(storeId, categoryId, referenceDate, 365);
	}
}

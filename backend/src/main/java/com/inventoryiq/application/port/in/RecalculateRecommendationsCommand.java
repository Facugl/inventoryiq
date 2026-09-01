package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de RecalculateRecommendationsUseCase (Sección 8.6).
 * Primer "Command" (en vez de "Query") de este proyecto: a diferencia de
 * los demás casos de uso, este escribe estado, no solo lee.
 *
 * La Sección 8.6 permite omitir sucursal_id para recalcular en todas las
 * sucursales — no se puede honrar esa parte todavía: no existe
 * StoreRepository (nunca se implementó 8.12), mismo motivo por el que
 * storeId es obligatorio en el resto del proyecto (single-store scope).
 *
 * referenceDate no aparece en el body documentado por la Sección 8.6
 * (que no expone ningún parámetro de fecha): la resuelve el adaptador
 * REST con la fecha del sistema al momento de la request, nunca el propio
 * caso de uso, siguiendo el mismo criterio del resto del proyecto ("el
 * caso de uso nunca lee el reloj del sistema").
 */
public record RecalculateRecommendationsCommand(Long storeId, LocalDate referenceDate) {

	public RecalculateRecommendationsCommand {
		if (storeId == null) {
			throw new InvalidDomainDataException(
					"storeId is required in this vertical slice (single-store scope, see class Javadoc)");
		}

		if (referenceDate == null) {
			throw new InvalidDomainDataException("referenceDate is required; the use case never reads the system clock");
		}
	}
}

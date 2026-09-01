package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de RecalculateProductStatusUseCase (Sección 9.10).
 *
 * A diferencia de todos los demás casos de uso de este proyecto,
 * storeId es OPCIONAL acá: null significa "todas las sucursales
 * activas" (Sección 9.10, "Entradas: ninguna... o alcance específico si
 * se dispara manualmente" — es el propio job programado el que resuelve
 * "todo el catálogo" por defecto). No se retrofittea este mismo criterio
 * en los casos de uso existentes (GetCriticalProducts, DetectOverstock,
 * GenerateAlerts, GenerateReorderSuggestions, RecalculateRecommendations
 * siguen exigiendo storeId): este comando hace su propio loop sobre
 * sucursales activas y los invoca una vez por sucursal.
 *
 * referenceDate sigue el mismo criterio que el resto del proyecto: nunca
 * se resuelve con LocalDate.now() acá — lo hace el adaptador que dispara
 * este caso de uso (el trigger programado o el controller manual).
 */
public record RecalculateProductStatusCommand(Long storeId, LocalDate referenceDate) {

	public RecalculateProductStatusCommand {
		if (referenceDate == null) {
			throw new InvalidDomainDataException("referenceDate is required; the use case never reads the system clock");
		}
	}
}

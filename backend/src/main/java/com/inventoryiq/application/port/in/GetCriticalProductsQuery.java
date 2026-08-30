package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de GetCriticalProductsUseCase.
 *
 * storeId es OBLIGATORIO en esta primera versión del slice: es una
 * limitación explícita del vertical slice inicial (evaluar una sucursal
 * por invocación), no una modificación del contrato funcional de la
 * Sección 8.3 de la documentación, que lo declara opcional ("todas las
 * sucursales" si se omite). Soportar el caso "todas las sucursales"
 * requiere poder enumerar sucursales activas (StoreRepository), que
 * todavía no existe — se agrega en una iteración posterior sin tocar
 * este contrato: bastará con permitir storeId=null y, en ese caso,
 * iterar internamente sobre todas las sucursales activas.
 *
 * referenceDate también es obligatorio y nunca se resuelve con
 * LocalDate.now(): los CSV son una simulación histórica con fecha de
 * corte fija (2026-08-01), así que quien invoca el caso de uso decide
 * qué "hoy" usar.
 */
public record GetCriticalProductsQuery(
		Long storeId,
		Long categoryId,
		Integer limit,
		LocalDate referenceDate,
		int salesWindowDays) {

	public GetCriticalProductsQuery {
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

		if (limit != null && limit <= 0) {
			throw new InvalidDomainDataException(
					"limit must be greater than 0 when provided, received: " + limit);
		}
	}

	/** Ventana de ventas por defecto para ADS y para el valor de venta del ABC: 90 días (Sección 4.1). */
	public static GetCriticalProductsQuery of(Long storeId, Long categoryId, Integer limit, LocalDate referenceDate) {
		return new GetCriticalProductsQuery(storeId, categoryId, limit, referenceDate, 90);
	}
}

package com.inventoryiq.domain.model;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Sección 5.7 — Snapshot diario de stock de un producto en una sucursal.
 *
 * inventoryId es nullable a propósito: un snapshot recién construido
 * (por ejemplo, una fila de un CSV de inventario subido) todavía no
 * tiene id — lo asigna el adaptador de persistencia al guardarlo.
 */
public record Inventory(
		Long inventoryId,
		LocalDate snapshotDate,
		Long productId,
		Long storeId,
		int currentStock,
		int stockInTransit) {

	public Inventory {
		if (currentStock < 0) {
			throw new InvalidDomainDataException("Current stock cannot be negative, received: " + currentStock);
		}

		if (stockInTransit < 0) {
			throw new InvalidDomainDataException("Stock in transit cannot be negative, received: " + stockInTransit);
		}
	}
}

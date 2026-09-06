package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

import java.time.LocalDate;

/**
 * Parámetros de entrada de RecordInventoryCountUseCase. Sin endpoint
 * documentado en la Sección 8 — cubre el conteo físico manual de stock
 * (por ejemplo, antes de emitir un pedido a un proveedor) descrito como
 * el flujo real de reposición del negocio, ninguna importación desde el
 * POS.
 *
 * stockInTransit no es un dato que un conteo físico pueda relevar (nadie
 * cuenta "lo que está en camino" parado en el depósito): esta versión
 * siempre lo asume en 0, una simplificación deliberada — RecordInventoryCountService
 * no recibe ese valor.
 */
public record RecordInventoryCountCommand(Long productId, Long storeId, LocalDate countDate, int countedStock) {

	public RecordInventoryCountCommand {
		if (productId == null) {
			throw new InvalidDomainDataException("productId is required");
		}

		if (storeId == null) {
			throw new InvalidDomainDataException("storeId is required");
		}

		if (countDate == null) {
			throw new InvalidDomainDataException("countDate is required; the use case never reads the system clock");
		}

		if (countedStock < 0) {
			throw new InvalidDomainDataException("countedStock must be >= 0, received: " + countedStock);
		}
	}
}

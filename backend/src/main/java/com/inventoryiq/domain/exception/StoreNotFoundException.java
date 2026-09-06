package com.inventoryiq.domain.exception;

/** Se lanza cuando se busca una sucursal por id y no existe. */
public class StoreNotFoundException extends NotFoundException {
	public StoreNotFoundException(Long storeId) {
		super("Store not found: " + storeId);
	}
}

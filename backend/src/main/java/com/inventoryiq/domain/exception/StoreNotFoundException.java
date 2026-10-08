package com.inventoryiq.domain.exception;

/** Se lanza cuando se busca una sucursal por id y no existe. */
public class StoreNotFoundException extends NotFoundException {
	public StoreNotFoundException(Long storeId) {
		super(ErrorCode.STORE_NOT_FOUND, storeId, "Store not found: " + storeId);
	}
}

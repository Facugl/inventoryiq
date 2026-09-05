package com.inventoryiq.application.port.in;

/** Salida de ListStoresUseCase — solo lo que un selector de sucursal necesita. */
public record StoreResult(Long storeId, String name) {
}

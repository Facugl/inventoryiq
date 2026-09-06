package com.inventoryiq.domain.exception;

/** Se lanza cuando se busca un proveedor por id y no existe. */
public class SupplierNotFoundException extends NotFoundException {
	public SupplierNotFoundException(Long supplierId) {
		super("Supplier not found: " + supplierId);
	}
}

package com.inventoryiq.domain.exception;

/** Se lanza cuando se busca un producto por id y no existe en el catálogo. */
public class ProductNotFoundException extends NotFoundException {
	public ProductNotFoundException(Long productId) {
		super(ErrorCode.PRODUCT_NOT_FOUND, productId, "Product not found: " + productId);
	}
}

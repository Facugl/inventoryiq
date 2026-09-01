package com.inventoryiq.domain.exception;

/**
 * Se lanza cuando se busca un producto por id y no existe en el catálogo.
 * A diferencia de InvalidDomainDataException (datos de entrada inválidos,
 * 400), esta representa un recurso inexistente y se traduce a 404 en
 * GlobalExceptionHandler — la primera vez que este proyecto necesita esa
 * distinción, porque ForecastDemand es el primer caso de uso escopeado a
 * un único producto (lookup por id) en vez de a un catálogo completo.
 */
public class ProductNotFoundException extends RuntimeException {
	public ProductNotFoundException(Long productId) {
		super("Product not found: " + productId);
	}
}

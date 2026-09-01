package com.inventoryiq.domain.exception;

/**
 * Se lanza cuando se busca un recurso por id y no existe. A diferencia de
 * InvalidDomainDataException (datos de entrada inválidos, 400), esta
 * representa un recurso inexistente y se traduce a 404 en
 * GlobalExceptionHandler — con un único handler para todas las subclases.
 */
public abstract class NotFoundException extends RuntimeException {
	protected NotFoundException(String message) {
		super(message);
	}
}

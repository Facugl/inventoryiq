package com.inventoryiq.domain.exception;

/**
 * Se lanza cuando un dato de entrada viola una regla de negocio del dominio
 * (por ejemplo, un lead time negativo o una lista de ventas vacía).
 * Es una RuntimeException a propósito: en el dominio, estos errores son
 * bugs del llamador (un adaptador que no validó antes de invocar), no
 * condiciones esperables que deban forzar un try/catch en cada uso.
 */
public class InvalidDomainDataException extends RuntimeException {
	public InvalidDomainDataException(String mensaje) {
		super(mensaje);
	}
}

package com.inventoryiq.domain.exception;

/**
 * Se lanza cuando un dato de entrada viola una regla de negocio del dominio
 * (por ejemplo, un lead time negativo o una lista de ventas vacía).
 * Es una RuntimeException a propósito: en el dominio, estos errores son
 * bugs del llamador (un adaptador que no validó antes de invocar), no
 * condiciones esperables que deban forzar un try/catch en cada uso.
 *
 * El ErrorCode es opcional y solo lo llevan las validaciones que un
 * usuario puede disparar desde un formulario — ver ErrorCode para el
 * criterio. Cuando está, GlobalExceptionHandler lo publica y el cliente
 * redacta el mensaje en su idioma; cuando no, queda el mensaje en inglés,
 * que es material de log.
 */
public class InvalidDomainDataException extends RuntimeException {

	private final ErrorCode code;

	public InvalidDomainDataException(String mensaje) {
		this(null, mensaje);
	}

	public InvalidDomainDataException(ErrorCode code, String mensaje) {
		super(mensaje);
		this.code = code;
	}

	/** Puede ser null: ver el Javadoc de la clase. */
	public ErrorCode getCode() {
		return code;
	}
}

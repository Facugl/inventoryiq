package com.inventoryiq.domain.exception;

/**
 * Se lanza cuando se busca un recurso por id y no existe. A diferencia de
 * InvalidDomainDataException (datos de entrada inválidos, 400), esta
 * representa un recurso inexistente y se traduce a 404 en
 * GlobalExceptionHandler — con un único handler para todas las subclases.
 *
 * Además del mensaje (en inglés, como el resto de las excepciones del
 * backend, y destinado a logs) cada subclase aporta un ErrorCode y el id
 * que no se encontró. GlobalExceptionHandler los publica como
 * propiedades de la respuesta para que el cliente pueda redactar el
 * mensaje en su propio idioma sin parsear texto.
 */
public abstract class NotFoundException extends RuntimeException {

	private final ErrorCode code;
	private final Object resourceId;

	protected NotFoundException(ErrorCode code, Object resourceId, String message) {
		super(message);
		this.code = code;
		this.resourceId = resourceId;
	}

	public ErrorCode getCode() {
		return code;
	}

	public Object getResourceId() {
		return resourceId;
	}
}

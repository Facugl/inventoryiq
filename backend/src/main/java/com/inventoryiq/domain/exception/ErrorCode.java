package com.inventoryiq.domain.exception;

/**
 * Identificador estable de una condición de error, pensado para que lo
 * consuma una máquina y no una persona.
 *
 * Existe porque el mensaje de las excepciones de dominio está en inglés
 * (convención de todo el backend) y viajaba tal cual hasta la pantalla
 * del usuario, en una UI que por lo demás está en español. Con un código
 * aparte, cada cliente decide cómo redactarlo: el frontend lo traduce
 * (ver shared/apiError.ts) y el `detail` del ProblemDetail queda como
 * dato técnico para logs.
 *
 * El nombre de la constante ES el contrato con los clientes: se serializa
 * literal en la propiedad `code` de la respuesta de error, así que
 * renombrar una rompe a quien la consuma.
 *
 * Este enum no es la lista completa de códigos posibles: "VALIDATION_FAILED"
 * lo emite GlobalExceptionHandler cuando la request no pasa la bean
 * validation, una condición que no nace en el dominio —que no sabe que
 * existe HTTP— y por eso vive en el adaptador. El espejo del frontend
 * (shared/apiError.ts) cubre los dos orígenes.
 */
public enum ErrorCode {

	// --- Recurso inexistente (404) ---
	PRODUCT_NOT_FOUND,
	STORE_NOT_FOUND,
	SUPPLIER_NOT_FOUND,
	CATEGORY_NOT_FOUND,
	RECOMMENDATION_NOT_FOUND,

	/*
	 * Dato de entrada inválido (400). Solo llevan código las validaciones
	 * que un usuario puede efectivamente disparar desde la UI, y hoy esa
	 * lista tiene un solo elemento.
	 *
	 * El resto de InvalidDomainDataException no es alcanzable por HTTP: o
	 * son invariantes internas del dominio (ADS, estacionalidad, rotación),
	 * o las ataja antes la bean validation de la request —los DTO y los
	 * @RequestParam ya declaran @Positive/@PositiveOrZero sobre los mismos
	 * campos—, con lo que la validación del command nunca llega a correr
	 * desde un request real. Esas quedan sin código y con su mensaje en
	 * inglés, que es para quien lee un log.
	 *
	 * DATE_RANGE_INVALID es la excepción porque es una regla entre dos
	 * campos (fromDate contra toDate), algo que las anotaciones sueltas de
	 * cada parámetro no pueden expresar: llega entera al command, y la
	 * pantalla de inicio no restringe un rango invertido.
	 */
	DATE_RANGE_INVALID
}

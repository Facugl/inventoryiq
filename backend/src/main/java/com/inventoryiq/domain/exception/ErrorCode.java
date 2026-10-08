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
 */
public enum ErrorCode {
	PRODUCT_NOT_FOUND,
	STORE_NOT_FOUND,
	SUPPLIER_NOT_FOUND,
	CATEGORY_NOT_FOUND,
	RECOMMENDATION_NOT_FOUND
}

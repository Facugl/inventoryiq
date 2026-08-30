package com.inventoryiq.adapters.out.csv.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Helpers de parseo compartidos por los adaptadores CSV. Centraliza las
 * particularidades reales de los CSV simulados: booleanos "True"/"False"
 * (estilo Python), fechas ISO (yyyy-MM-dd), campos nullable (vacío = null)
 * y umbrales de categorías que llegan como decimal ("20.0") aunque el
 * dominio los modela como int.
 */
public final class CsvFieldParsers {
	private CsvFieldParsers() {
	}

	public static LocalDate parseDate(String value) {
		return LocalDate.parse(value.trim());
	}

	public static BigDecimal parseDecimal(String value) {
		return new BigDecimal(value.trim());
	}

	public static boolean parseBoolean(String value) {
		return Boolean.parseBoolean(value.trim());
	}

	public static int parseInt(String value) {
		return Integer.parseInt(value.trim());
	}

	public static long parseLong(String value) {
		return Long.parseLong(value.trim());
	}

	public static Long parseNullableLong(String value) {
		return isBlank(value) ? null : Long.parseLong(value.trim());
	}

	/**
	 * categorias.csv trae umbrales como "20.0"; el dominio (Category) los
	 * modela como int. Falla explícitamente si el valor no es un entero
	 * exacto, en vez de truncar en silencio un dato inesperado.
	 */
	public static int parseIntFromDecimal(String value) {
		double parsed = Double.parseDouble(value.trim());
		if (parsed != Math.rint(parsed)) {
			throw new IllegalStateException("Expected an integer-valued number but got: " + value);
		}
		return (int) parsed;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

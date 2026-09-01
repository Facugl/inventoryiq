package com.inventoryiq.application.port.in;

/**
 * Tipo de archivo de una ingesta CSV — Sección 8.13. Solo SALES está
 * implementado en este slice (ver Javadoc de IngestCsvFileUseCase);
 * compras/inventario/productos quedan fuera de alcance.
 */
public enum CsvFileType {
	SALES
}

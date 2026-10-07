package com.inventoryiq.application.port.in;

/**
 * Tipo de archivo de una ingesta CSV — Sección 8.13. Hay implementación
 * para SALES e INVENTORY (ver Javadoc de IngestCsvFileUseCase);
 * compras/productos/proveedores/categorías/sucursales quedan fuera de
 * alcance.
 */
public enum CsvFileType {
	SALES,
	INVENTORY
}

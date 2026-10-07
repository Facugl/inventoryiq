package com.inventoryiq.application.port.in;

/**
 * Puerto de entrada — Sección 8.13/9.9. Hay implementación para
 * CsvFileType.SALES y CsvFileType.INVENTORY. En los dos casos cada fila
 * candidata se valida contra ProductRepository (producto_id existe),
 * StoreRepository (sucursal_id existe) y el puerto de ingesta que
 * corresponda —SaleIngestionRepository o InventoryIngestionRepository—
 * para descartar duplicados por producto+sucursal+fecha (Sección 7.3),
 * y se persiste si pasa las tres. Lanza
 * CsvIngestionThresholdExceededException (422) si el % de filas
 * rechazadas —sumando lo que ya venía rechazado del parseo— supera el
 * umbral crítico (Sección 7.4): en ese caso no se persiste nada del lote.
 *
 * Diferencia entre ambos tipos: una venta ingerida trae su propio
 * venta_id en la fila, mientras que el inventario_id de un snapshot lo
 * asigna el adaptador de persistencia (ver InventoryIngestionRepository).
 */
public interface IngestCsvFileUseCase {

	IngestCsvFileResult execute(IngestCsvFileCommand command);
}

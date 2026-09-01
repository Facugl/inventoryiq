package com.inventoryiq.application.port.in;

/**
 * Puerto de entrada — Sección 8.13/9.9. Solo CsvFileType.SALES está
 * implementado: cada fila candidata se valida contra ProductRepository
 * (producto_id existe), StoreRepository (sucursal_id existe) y
 * SaleIngestionRepository (no es un duplicado por producto+sucursal+
 * fecha, Sección 7.3), y se persiste si pasa las tres. Lanza
 * CsvIngestionThresholdExceededException (422) si el % de filas
 * rechazadas —sumando lo que ya venía rechazado del parseo— supera el
 * umbral crítico (Sección 7.4): en ese caso no se persiste nada del lote.
 */
public interface IngestCsvFileUseCase {

	IngestCsvFileResult execute(IngestCsvFileCommand command);
}

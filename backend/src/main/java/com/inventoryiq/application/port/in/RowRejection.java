package com.inventoryiq.application.port.in;

/** Una fila de un CSV ingerido que no pasó validación (Sección 9.9), con el motivo. rowNumber es 1-based sobre las filas de datos (sin contar el header). */
public record RowRejection(int rowNumber, String reason) {
}

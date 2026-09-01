package com.inventoryiq.adapters.in.rest.dto;

/** Forma JSON pública de una fila rechazada de una ingesta CSV (Sección 8.13). */
public record RowRejectionResponse(int rowNumber, String reason) {
}

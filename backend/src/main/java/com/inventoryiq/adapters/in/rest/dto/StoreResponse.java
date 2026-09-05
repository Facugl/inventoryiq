package com.inventoryiq.adapters.in.rest.dto;

/** Forma JSON pública de una sucursal activa (GET /api/v1/stores). */
public record StoreResponse(Long storeId, String name) {
}

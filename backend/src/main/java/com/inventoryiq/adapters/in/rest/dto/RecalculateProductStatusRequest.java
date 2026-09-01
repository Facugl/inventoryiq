package com.inventoryiq.adapters.in.rest.dto;

import jakarta.validation.constraints.Positive;

/** Cuerpo de la request de POST /api/v1/product-status/recalculate (Sección 9.10). storeId es opcional: si se omite, procesa todas las sucursales activas. */
public record RecalculateProductStatusRequest(@Positive Long storeId) {
}

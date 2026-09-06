package com.inventoryiq.adapters.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Cuerpo de la request de POST /api/v1/inventory-snapshots. */
public record RecordInventoryCountRequest(
		@NotNull @Positive Long productId,
		@NotNull @Positive Long storeId,
		@NotNull @PositiveOrZero Integer stockActual) {
}

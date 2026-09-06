package com.inventoryiq.adapters.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuerpo de la request de PATCH /api/v1/suppliers/{supplierId}/lead-time. */
public record UpdateSupplierLeadTimeRequest(@NotNull @Positive Integer leadTimeDays) {
}

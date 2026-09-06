package com.inventoryiq.adapters.in.rest.dto;

/** Forma JSON pública de un proveedor activo (GET/PATCH /api/v1/suppliers). */
public record SupplierResponse(Long supplierId, String businessName, int leadTimeDays, String paymentTerms) {
}

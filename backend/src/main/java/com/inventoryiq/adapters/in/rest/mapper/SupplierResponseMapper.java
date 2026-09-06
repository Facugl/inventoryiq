package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.SupplierResponse;
import com.inventoryiq.application.port.in.SupplierResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class SupplierResponseMapper {
	private SupplierResponseMapper() {
	}

	public static SupplierResponse toResponse(SupplierResult result) {
		return new SupplierResponse(result.supplierId(), result.businessName(), result.leadTimeDays(), result.paymentTerms());
	}
}

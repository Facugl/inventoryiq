package com.inventoryiq.application.port.in;

/** Resultado de aplicación de ListSuppliersUseCase / UpdateSupplierLeadTimeUseCase. */
public record SupplierResult(
		Long supplierId,
		String businessName,
		int leadTimeDays,
		String paymentTerms) {
}

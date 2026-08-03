package com.inventoryiq.domain.model;

import com.inventoryiq.domain.model.vo.LeadTime;

/** Sección 5.3 — Proveedor de productos. */
public record Supplier(
		Long supplierId,
		String businessName,
		LeadTime averageLeadTime,
		String paymentTerms,
		boolean active) {
}

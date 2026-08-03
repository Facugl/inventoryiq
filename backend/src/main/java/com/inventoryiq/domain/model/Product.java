package com.inventoryiq.domain.model;

import com.inventoryiq.domain.model.vo.LeadTime;

import java.math.BigDecimal;

/** Sección 5.1 — Catálogo de productos. */
public record Product(
		Long productId,
		String sku,
		String name,
		Long categoryId,
		Long supplierId,
		String unitOfMeasure,
		BigDecimal costPrice,
		BigDecimal sellingPrice,
		LeadTime leadTime,
		boolean active) {
}
package com.inventoryiq.adapters.in.rest.dto;

import com.inventoryiq.domain.model.ProductStatus;

/**
 * Forma JSON pública de un producto crítico (GET /api/v1/products/critical).
 * Deliberadamente distinto de CriticalProductResult (application/port/in):
 * este DTO es el contrato de la API REST, no debe cambiar solo porque
 * cambie la forma interna de un value object del dominio. Por eso los VOs
 * (ReorderPoint, CriticalityLevel) se aplanan a double en vez de anidarse
 * tal cual. ProductStatus sí se reutiliza directamente: es un enum de
 * dominio, y un adaptador puede depender de domain (dirección permitida).
 */
public record CriticalProductResponse(
		Long productId,
		String sku,
		String productName,
		Long storeId,
		Long categoryId,
		int currentStock,
		double reorderPointUnits,
		double currentDaysOfCoverage,
		ProductStatus status,
		double criticalityScore) {
}

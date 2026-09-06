package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.ProductSummaryResponse;
import com.inventoryiq.application.port.in.ProductSummaryResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class ProductSummaryResponseMapper {
	private ProductSummaryResponseMapper() {
	}

	public static ProductSummaryResponse toResponse(ProductSummaryResult result) {
		return new ProductSummaryResponse(result.productId(), result.sku(), result.name(), result.categoryId());
	}
}

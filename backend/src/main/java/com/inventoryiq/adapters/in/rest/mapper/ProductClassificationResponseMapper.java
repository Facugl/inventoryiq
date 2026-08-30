package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.ProductClassificationResponse;
import com.inventoryiq.application.port.in.ProductClassificationResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class ProductClassificationResponseMapper {
	private ProductClassificationResponseMapper() {
	}

	public static ProductClassificationResponse toResponse(ProductClassificationResult result) {
		return new ProductClassificationResponse(
				result.productId(),
				result.sku(),
				result.productName(),
				result.storeId(),
				result.categoryId(),
				result.abcClass(),
				result.xyzClass());
	}
}

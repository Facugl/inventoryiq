package com.inventoryiq.adapters.in.rest.mapper;

import com.inventoryiq.adapters.in.rest.dto.StoreResponse;
import com.inventoryiq.application.port.in.StoreResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
public final class StoreResponseMapper {
	private StoreResponseMapper() {
	}

	public static StoreResponse toResponse(StoreResult result) {
		return new StoreResponse(result.storeId(), result.name());
	}
}

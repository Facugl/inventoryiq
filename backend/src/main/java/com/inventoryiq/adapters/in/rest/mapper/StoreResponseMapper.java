package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.StoreResponse;
import com.inventoryiq.application.port.in.StoreResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StoreResponseMapper {
	StoreResponseMapper INSTANCE = Mappers.getMapper(StoreResponseMapper.class);

	StoreResponse toResponse(StoreResult result);
}

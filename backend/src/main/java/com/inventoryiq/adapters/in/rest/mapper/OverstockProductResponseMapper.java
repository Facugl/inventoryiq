package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.OverstockProductResponse;
import com.inventoryiq.application.port.in.OverstockProductResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OverstockProductResponseMapper {
	OverstockProductResponseMapper INSTANCE = Mappers.getMapper(OverstockProductResponseMapper.class);

	OverstockProductResponse toResponse(OverstockProductResult result);
}

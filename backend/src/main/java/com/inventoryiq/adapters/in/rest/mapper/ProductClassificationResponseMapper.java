package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.ProductClassificationResponse;
import com.inventoryiq.application.port.in.ProductClassificationResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductClassificationResponseMapper {
	ProductClassificationResponseMapper INSTANCE = Mappers.getMapper(ProductClassificationResponseMapper.class);

	ProductClassificationResponse toResponse(ProductClassificationResult result);
}

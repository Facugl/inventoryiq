package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.CategoryResponse;
import com.inventoryiq.application.port.in.CategoryResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryResponseMapper {
	CategoryResponseMapper INSTANCE = Mappers.getMapper(CategoryResponseMapper.class);

	CategoryResponse toResponse(CategoryResult result);
}

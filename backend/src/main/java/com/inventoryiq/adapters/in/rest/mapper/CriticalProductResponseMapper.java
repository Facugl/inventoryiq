package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.CriticalProductResponse;
import com.inventoryiq.application.port.in.CriticalProductResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CriticalProductResponseMapper {
	CriticalProductResponseMapper INSTANCE = Mappers.getMapper(CriticalProductResponseMapper.class);

	@Mapping(target = "reorderPointUnits", source = "reorderPoint.units")
	@Mapping(target = "criticalityScore", source = "criticalityLevel.score")
	CriticalProductResponse toResponse(CriticalProductResult result);
}

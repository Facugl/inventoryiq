package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.AlertResponse;
import com.inventoryiq.application.port.in.AlertResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AlertResponseMapper {
	AlertResponseMapper INSTANCE = Mappers.getMapper(AlertResponseMapper.class);

	AlertResponse toResponse(AlertResult result);
}

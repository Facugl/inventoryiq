package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.ReorderSuggestionResponse;
import com.inventoryiq.application.port.in.ReorderSuggestionResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReorderSuggestionResponseMapper {
	ReorderSuggestionResponseMapper INSTANCE = Mappers.getMapper(ReorderSuggestionResponseMapper.class);

	ReorderSuggestionResponse toResponse(ReorderSuggestionResult result);
}

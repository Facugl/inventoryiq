package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.RecommendationResponse;
import com.inventoryiq.application.port.in.RecommendationResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RecommendationResponseMapper {
	RecommendationResponseMapper INSTANCE = Mappers.getMapper(RecommendationResponseMapper.class);

	RecommendationResponse toResponse(RecommendationResult result);
}

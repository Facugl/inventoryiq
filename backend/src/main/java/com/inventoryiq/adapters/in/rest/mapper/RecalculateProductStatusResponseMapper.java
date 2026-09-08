package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.RecalculateProductStatusResponse;
import com.inventoryiq.adapters.in.rest.dto.RecalculateRecommendationsResponse;
import com.inventoryiq.adapters.in.rest.dto.StoreRecalculationSummaryResponse;
import com.inventoryiq.application.port.in.RecalculateProductStatusResult;
import com.inventoryiq.application.port.in.RecalculateRecommendationsResult;
import com.inventoryiq.application.port.in.StoreRecalculationSummary;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RecalculateProductStatusResponseMapper {
	RecalculateProductStatusResponseMapper INSTANCE = Mappers.getMapper(RecalculateProductStatusResponseMapper.class);

	RecalculateProductStatusResponse toResponse(RecalculateProductStatusResult result);

	StoreRecalculationSummaryResponse toResponse(StoreRecalculationSummary summary);

	RecalculateRecommendationsResponse toResponse(RecalculateRecommendationsResult result);
}

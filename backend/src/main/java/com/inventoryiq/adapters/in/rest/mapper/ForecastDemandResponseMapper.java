package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.DemandForecastPeriodResponse;
import com.inventoryiq.adapters.in.rest.dto.ForecastDemandResponse;
import com.inventoryiq.application.port.in.DemandForecastPeriod;
import com.inventoryiq.application.port.in.ForecastDemandResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ForecastDemandResponseMapper {
	ForecastDemandResponseMapper INSTANCE = Mappers.getMapper(ForecastDemandResponseMapper.class);

	ForecastDemandResponse toResponse(ForecastDemandResult result);

	DemandForecastPeriodResponse toResponse(DemandForecastPeriod period);
}

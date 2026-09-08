package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.ProductSummaryResponse;
import com.inventoryiq.application.port.in.ProductSummaryResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductSummaryResponseMapper {
	ProductSummaryResponseMapper INSTANCE = Mappers.getMapper(ProductSummaryResponseMapper.class);

	ProductSummaryResponse toResponse(ProductSummaryResult result);
}

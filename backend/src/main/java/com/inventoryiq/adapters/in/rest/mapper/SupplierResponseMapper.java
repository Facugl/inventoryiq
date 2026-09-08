package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.SupplierResponse;
import com.inventoryiq.application.port.in.SupplierResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SupplierResponseMapper {
	SupplierResponseMapper INSTANCE = Mappers.getMapper(SupplierResponseMapper.class);

	SupplierResponse toResponse(SupplierResult result);
}

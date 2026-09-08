package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.InventoryKPIsResponse;
import com.inventoryiq.application.port.in.InventoryKPIsResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InventoryKPIsResponseMapper {
	InventoryKPIsResponseMapper INSTANCE = Mappers.getMapper(InventoryKPIsResponseMapper.class);

	InventoryKPIsResponse toResponse(InventoryKPIsResult result);
}

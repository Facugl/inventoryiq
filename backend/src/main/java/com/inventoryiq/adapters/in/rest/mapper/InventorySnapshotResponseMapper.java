package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.InventorySnapshotResponse;
import com.inventoryiq.application.port.in.InventorySnapshotResult;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InventorySnapshotResponseMapper {
	InventorySnapshotResponseMapper INSTANCE = Mappers.getMapper(InventorySnapshotResponseMapper.class);

	InventorySnapshotResponse toResponse(InventorySnapshotResult result);
}

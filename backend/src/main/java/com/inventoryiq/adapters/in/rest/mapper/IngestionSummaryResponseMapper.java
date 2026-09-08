package com.inventoryiq.adapters.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.inventoryiq.adapters.in.rest.dto.IngestionSummaryResponse;
import com.inventoryiq.adapters.in.rest.dto.RowRejectionResponse;
import com.inventoryiq.application.port.in.IngestCsvFileResult;
import com.inventoryiq.application.port.in.RowRejection;

/** Traduce la salida del caso de uso (application) al DTO público de la API REST. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IngestionSummaryResponseMapper {
	IngestionSummaryResponseMapper INSTANCE = Mappers.getMapper(IngestionSummaryResponseMapper.class);

	IngestionSummaryResponse toResponse(IngestCsvFileResult result);

	RowRejectionResponse toResponse(RowRejection rejection);
}

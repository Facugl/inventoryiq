package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.StoreResponse;
import com.inventoryiq.adapters.in.rest.mapper.StoreResponseMapper;
import com.inventoryiq.application.port.in.ListStoresUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST para ListStoresUseCase. Sin endpoint
 * documentado en la Sección 8 — se agrega porque ningún otro endpoint del
 * proyecto permite descubrir qué sucursales existen; hasta ahora todo
 * cliente tenía que asumirlas de antemano (single-store scope con
 * storeId como parámetro obligatorio en el resto del proyecto).
 */
@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "Sucursales", description = "Catálogo de puntos de venta activos")
public class StoresController {

	private final ListStoresUseCase listStoresUseCase;

	public StoresController(ListStoresUseCase listStoresUseCase) {
		this.listStoresUseCase = listStoresUseCase;
	}

	@GetMapping
	@Operation(summary = "Listar sucursales activas")
	public List<StoreResponse> getStores() {
		return listStoresUseCase.execute().stream()
				.map(StoreResponseMapper.INSTANCE::toResponse)
				.toList();
	}
}

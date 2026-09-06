package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.InventorySnapshotResponse;
import com.inventoryiq.adapters.in.rest.dto.RecordInventoryCountRequest;
import com.inventoryiq.adapters.in.rest.mapper.InventorySnapshotResponseMapper;
import com.inventoryiq.application.port.in.RecordInventoryCountCommand;
import com.inventoryiq.application.port.in.RecordInventoryCountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Adaptador de entrada REST para RecordInventoryCountUseCase. Sin endpoint
 * documentado en la Sección 8. countDate no es un parámetro de request
 * (un conteo físico siempre es "de hoy"): se resuelve acá con el reloj
 * inyectado, mismo criterio que RecommendationsController.
 */
@RestController
@RequestMapping("/api/v1/inventory-snapshots")
@Validated
@Tag(name = "Inventario", description = "Registro manual de conteos físicos de stock (pantalla de Administración)")
public class InventorySnapshotsController {

	private final RecordInventoryCountUseCase recordInventoryCountUseCase;
	private final Clock clock;

	public InventorySnapshotsController(RecordInventoryCountUseCase recordInventoryCountUseCase, Clock clock) {
		this.recordInventoryCountUseCase = recordInventoryCountUseCase;
		this.clock = clock;
	}

	@PostMapping
	@Operation(summary = "Registrar un conteo físico de stock",
			description = "Crea un nuevo snapshot de inventario fechado hoy. stockInTransit siempre se persiste en 0: "
					+ "un conteo físico no puede relevar lo que está en camino de un proveedor.")
	public InventorySnapshotResponse recordCount(@RequestBody @Valid RecordInventoryCountRequest request) {
		LocalDate countDate = LocalDate.now(clock);

		RecordInventoryCountCommand command = new RecordInventoryCountCommand(
				request.productId(), request.storeId(), countDate, request.stockActual());

		return InventorySnapshotResponseMapper.toResponse(recordInventoryCountUseCase.execute(command));
	}
}

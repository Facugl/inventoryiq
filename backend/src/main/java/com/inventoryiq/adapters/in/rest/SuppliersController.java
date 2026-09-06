package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.adapters.in.rest.dto.SupplierResponse;
import com.inventoryiq.adapters.in.rest.dto.UpdateSupplierLeadTimeRequest;
import com.inventoryiq.adapters.in.rest.mapper.SupplierResponseMapper;
import com.inventoryiq.application.port.in.ListSuppliersUseCase;
import com.inventoryiq.application.port.in.UpdateSupplierLeadTimeCommand;
import com.inventoryiq.application.port.in.UpdateSupplierLeadTimeUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST para ListSuppliersUseCase y
 * UpdateSupplierLeadTimeUseCase (Sección 8.11 — antes PLANIFICADO). Sin
 * ingesta automática: el catálogo de proveedores se carga y su lead time
 * se corrige a mano, porque no hay ningún sistema del que importarlo.
 */
@RestController
@RequestMapping("/api/v1/suppliers")
public class SuppliersController {

	private final ListSuppliersUseCase listSuppliersUseCase;
	private final UpdateSupplierLeadTimeUseCase updateSupplierLeadTimeUseCase;

	public SuppliersController(
			ListSuppliersUseCase listSuppliersUseCase, UpdateSupplierLeadTimeUseCase updateSupplierLeadTimeUseCase) {
		this.listSuppliersUseCase = listSuppliersUseCase;
		this.updateSupplierLeadTimeUseCase = updateSupplierLeadTimeUseCase;
	}

	@GetMapping
	public List<SupplierResponse> getSuppliers() {
		return listSuppliersUseCase.execute().stream()
				.map(SupplierResponseMapper::toResponse)
				.toList();
	}

	@PatchMapping("/{supplierId}/lead-time")
	public SupplierResponse updateLeadTime(
			@PathVariable Long supplierId, @RequestBody @Valid UpdateSupplierLeadTimeRequest request) {
		UpdateSupplierLeadTimeCommand command = new UpdateSupplierLeadTimeCommand(supplierId, request.leadTimeDays());
		return SupplierResponseMapper.toResponse(updateSupplierLeadTimeUseCase.execute(command));
	}
}

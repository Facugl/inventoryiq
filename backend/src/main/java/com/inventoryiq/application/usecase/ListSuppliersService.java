package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.ListSuppliersUseCase;
import com.inventoryiq.application.port.in.SupplierResult;
import com.inventoryiq.application.port.out.SupplierRepository;
import com.inventoryiq.domain.model.Supplier;

import java.util.List;

/** Implementación de ListSuppliersUseCase. Pura traducción: sin reglas de negocio propias. */
public class ListSuppliersService implements ListSuppliersUseCase {

	private final SupplierRepository supplierRepository;

	public ListSuppliersService(SupplierRepository supplierRepository) {
		this.supplierRepository = supplierRepository;
	}

	@Override
	public List<SupplierResult> execute() {
		return supplierRepository.findAllActive().stream()
				.map(ListSuppliersService::toResult)
				.toList();
	}

	private static SupplierResult toResult(Supplier supplier) {
		return new SupplierResult(
				supplier.supplierId(), supplier.businessName(), supplier.averageLeadTime().days(), supplier.paymentTerms());
	}
}

package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.SupplierResult;
import com.inventoryiq.application.port.in.UpdateSupplierLeadTimeCommand;
import com.inventoryiq.application.port.in.UpdateSupplierLeadTimeUseCase;
import com.inventoryiq.application.port.out.SupplierRepository;
import com.inventoryiq.domain.exception.SupplierNotFoundException;
import com.inventoryiq.domain.model.Supplier;
import com.inventoryiq.domain.model.vo.LeadTime;

/** Implementación de UpdateSupplierLeadTimeUseCase. */
public class UpdateSupplierLeadTimeService implements UpdateSupplierLeadTimeUseCase {

	private final SupplierRepository supplierRepository;

	public UpdateSupplierLeadTimeService(SupplierRepository supplierRepository) {
		this.supplierRepository = supplierRepository;
	}

	@Override
	public SupplierResult execute(UpdateSupplierLeadTimeCommand command) {
		supplierRepository.findById(command.supplierId())
				.orElseThrow(() -> new SupplierNotFoundException(command.supplierId()));

		Supplier updated = supplierRepository.updateLeadTime(command.supplierId(), new LeadTime(command.leadTimeDays()));

		return new SupplierResult(
				updated.supplierId(), updated.businessName(), updated.averageLeadTime().days(), updated.paymentTerms());
	}
}

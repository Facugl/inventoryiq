package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;

/**
 * Parámetros de entrada de UpdateSupplierLeadTimeUseCase. La validación de
 * leadTimeDays > 0 vive en LeadTime (Sección 4.2) — este record solo valida
 * lo que le compete a él, igual que RecordInventoryCountCommand.
 */
public record UpdateSupplierLeadTimeCommand(Long supplierId, int leadTimeDays) {

	public UpdateSupplierLeadTimeCommand {
		if (supplierId == null) {
			throw new InvalidDomainDataException("supplierId is required");
		}
	}
}

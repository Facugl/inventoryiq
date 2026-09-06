package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.SupplierResult;
import com.inventoryiq.application.port.in.UpdateSupplierLeadTimeCommand;
import com.inventoryiq.application.port.out.SupplierRepository;
import com.inventoryiq.domain.exception.SupplierNotFoundException;
import com.inventoryiq.domain.model.Supplier;
import com.inventoryiq.domain.model.vo.LeadTime;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica UpdateSupplierLeadTimeService: valida que el proveedor exista
 * antes de delegar en SupplierRepository.updateLeadTime() (mismo criterio
 * de integridad referencial que RecordInventoryCountService usa para
 * producto/sucursal) y traduce el resultado a SupplierResult.
 */
class UpdateSupplierLeadTimeServiceTest {

	@Test
	void updatesTheLeadTimeAndReturnsTheResultingSupplier() {
		FakeSupplierRepository suppliers = new FakeSupplierRepository();
		suppliers.add(new Supplier(1L, "Lácteos del Sur S.A.", new LeadTime(3), "30 días", true));

		UpdateSupplierLeadTimeService service = new UpdateSupplierLeadTimeService(suppliers);

		SupplierResult result = service.execute(new UpdateSupplierLeadTimeCommand(1L, 7));

		assertEquals(new SupplierResult(1L, "Lácteos del Sur S.A.", 7, "30 días"), result);
	}

	@Test
	void rejectsAnUnknownSupplier() {
		UpdateSupplierLeadTimeService service = new UpdateSupplierLeadTimeService(new FakeSupplierRepository());

		assertThrows(SupplierNotFoundException.class,
				() -> service.execute(new UpdateSupplierLeadTimeCommand(9999L, 7)));
	}

	private static class FakeSupplierRepository implements SupplierRepository {
		private final Map<Long, Supplier> suppliers = new HashMap<>();

		void add(Supplier supplier) {
			suppliers.put(supplier.supplierId(), supplier);
		}

		@Override
		public Optional<Supplier> findById(Long supplierId) {
			return Optional.ofNullable(suppliers.get(supplierId));
		}

		@Override
		public List<Supplier> findAllActive() {
			return suppliers.values().stream().filter(Supplier::active).toList();
		}

		@Override
		public Supplier updateLeadTime(Long supplierId, LeadTime newLeadTime) {
			Supplier existing = suppliers.get(supplierId);
			Supplier updated = new Supplier(
					existing.supplierId(), existing.businessName(), newLeadTime, existing.paymentTerms(), existing.active());
			suppliers.put(supplierId, updated);
			return updated;
		}
	}
}

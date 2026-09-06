package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.SupplierResult;
import com.inventoryiq.application.port.out.SupplierRepository;
import com.inventoryiq.domain.model.Supplier;
import com.inventoryiq.domain.model.vo.LeadTime;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica que ListSuppliersService delegue en SupplierRepository.findAllActive()
 * y traduzca cada Supplier a un SupplierResult — sin lógica de negocio
 * propia que testear más allá de esa traducción.
 */
class ListSuppliersServiceTest {

	@Test
	void returnsOnlyActiveSuppliersTranslatedToResults() {
		FakeSupplierRepository suppliers = new FakeSupplierRepository();
		suppliers.add(new Supplier(1L, "Lácteos del Sur S.A.", new LeadTime(3), "30 días", true));
		suppliers.add(new Supplier(2L, "Proveedor Dado de Baja", new LeadTime(4), "Contado", false));

		ListSuppliersService service = new ListSuppliersService(suppliers);

		List<SupplierResult> result = service.execute();

		assertEquals(List.of(new SupplierResult(1L, "Lácteos del Sur S.A.", 3, "30 días")), result);
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
			throw new UnsupportedOperationException("not exercised by this test");
		}
	}
}

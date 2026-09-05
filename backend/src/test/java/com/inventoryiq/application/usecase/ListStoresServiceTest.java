package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.StoreResult;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.model.Store;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica que ListStoresService delegue en StoreRepository.findAllActive()
 * (el mismo método que ya usa RecalculateProductStatusService) y traduzca
 * cada Store a un StoreResult — sin lógica de negocio propia que testear
 * más allá de esa traducción.
 */
class ListStoresServiceTest {

	@Test
	void returnsOnlyActiveStoresTranslatedToResults() {
		FakeStoreRepository stores = new FakeStoreRepository();
		stores.add(new Store(1L, "Sucursal Centro", "Av. Siempre Viva 742", true));
		stores.add(new Store(2L, "Sucursal Descontinuada", "Calle Falsa 123", false));

		ListStoresService service = new ListStoresService(stores);

		List<StoreResult> result = service.execute();

		assertEquals(List.of(new StoreResult(1L, "Sucursal Centro")), result);
	}

	private static class FakeStoreRepository implements StoreRepository {
		private final Map<Long, Store> stores = new HashMap<>();

		void add(Store store) {
			stores.put(store.storeId(), store);
		}

		@Override
		public Optional<Store> findById(Long storeId) {
			return Optional.ofNullable(stores.get(storeId));
		}

		@Override
		public List<Store> findAllActive() {
			return stores.values().stream().filter(Store::active).toList();
		}
	}
}

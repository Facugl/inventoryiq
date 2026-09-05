package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.ListStoresUseCase;
import com.inventoryiq.application.port.in.StoreResult;
import com.inventoryiq.application.port.out.StoreRepository;

import java.util.List;

/** Implementación de ListStoresUseCase. Pura traducción: sin reglas de negocio propias. */
public class ListStoresService implements ListStoresUseCase {

	private final StoreRepository storeRepository;

	public ListStoresService(StoreRepository storeRepository) {
		this.storeRepository = storeRepository;
	}

	@Override
	public List<StoreResult> execute() {
		return storeRepository.findAllActive().stream()
				.map(store -> new StoreResult(store.storeId(), store.name()))
				.toList();
	}
}

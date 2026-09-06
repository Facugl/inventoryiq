package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.InventorySnapshotResult;
import com.inventoryiq.application.port.in.RecordInventoryCountCommand;
import com.inventoryiq.application.port.in.RecordInventoryCountUseCase;
import com.inventoryiq.application.port.out.InventoryIngestionRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.exception.ProductNotFoundException;
import com.inventoryiq.domain.exception.StoreNotFoundException;
import com.inventoryiq.domain.model.Inventory;

/** Implementación de RecordInventoryCountUseCase. */
public class RecordInventoryCountService implements RecordInventoryCountUseCase {

	private final ProductRepository productRepository;
	private final StoreRepository storeRepository;
	private final InventoryIngestionRepository inventoryIngestionRepository;

	public RecordInventoryCountService(
			ProductRepository productRepository,
			StoreRepository storeRepository,
			InventoryIngestionRepository inventoryIngestionRepository) {
		this.productRepository = productRepository;
		this.storeRepository = storeRepository;
		this.inventoryIngestionRepository = inventoryIngestionRepository;
	}

	@Override
	public InventorySnapshotResult execute(RecordInventoryCountCommand command) {
		productRepository.findById(command.productId())
				.orElseThrow(() -> new ProductNotFoundException(command.productId()));
		storeRepository.findById(command.storeId())
				.orElseThrow(() -> new StoreNotFoundException(command.storeId()));

		Inventory snapshot = inventoryIngestionRepository.save(
				command.countDate(), command.productId(), command.storeId(), command.countedStock(), 0);

		return new InventorySnapshotResult(
				snapshot.inventoryId(), snapshot.snapshotDate(), snapshot.productId(), snapshot.storeId(),
				snapshot.currentStock(), snapshot.stockInTransit());
	}
}

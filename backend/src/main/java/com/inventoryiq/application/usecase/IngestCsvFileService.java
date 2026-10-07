package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CandidateRow;
import com.inventoryiq.application.port.in.IngestCsvFileCommand;
import com.inventoryiq.application.port.in.IngestCsvFileResult;
import com.inventoryiq.application.port.in.IngestCsvFileUseCase;
import com.inventoryiq.application.port.in.InventoryCandidateRow;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.application.port.out.InventoryIngestionRepository;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleIngestionRepository;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.exception.CsvIngestionThresholdExceededException;
import com.inventoryiq.domain.model.Inventory;
import com.inventoryiq.domain.model.Sale;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación de IngestCsvFileUseCase (Sección 9.9).
 *
 * Ver el Javadoc de IngestCsvFileCommand para la división de
 * responsabilidades con el parser del adaptador. Acá solo se validan las
 * reglas que necesitan puertos de aplicación (integridad referencial,
 * duplicados) y se aplica el umbral crítico de rechazo (Sección 7.4):
 * 5% del lote, tomado literalmente del ejemplo que da la propia sección
 * (no hay ningún otro valor sugerido en la documentación).
 *
 * El chequeo de duplicados cubre dos casos: contra lo ya persistido
 * (SaleIngestionRepository.existsByProductStoreAndDate /
 * InventoryIngestionRepository.existsByProductStoreAndSnapshotDate) y
 * entre filas del propio archivo subido (dos filas del mismo producto+
 * sucursal+fecha en un mismo lote) — lo segundo no lo detectaría el
 * repositorio por sí solo, porque nada se persiste hasta el final de la
 * corrida.
 *
 * SALES e INVENTORY tienen bucles separados en vez de uno genérico:
 * recorren tipos de dominio distintos (Sale / Inventory) y escriben por
 * puertos distintos. Lo que sí comparten —el cálculo del porcentaje de
 * rechazo y el corte todo-o-nada— vive en checkRejectionThreshold.
 */
public class IngestCsvFileService implements IngestCsvFileUseCase {

	private static final double REJECTION_THRESHOLD_PERCENT = 5.0;

	private final ProductRepository productRepository;
	private final StoreRepository storeRepository;
	private final SaleIngestionRepository saleIngestionRepository;
	private final InventoryIngestionRepository inventoryIngestionRepository;

	public IngestCsvFileService(
			ProductRepository productRepository,
			StoreRepository storeRepository,
			SaleIngestionRepository saleIngestionRepository,
			InventoryIngestionRepository inventoryIngestionRepository) {
		this.productRepository = productRepository;
		this.storeRepository = storeRepository;
		this.saleIngestionRepository = saleIngestionRepository;
		this.inventoryIngestionRepository = inventoryIngestionRepository;
	}

	@Override
	public IngestCsvFileResult execute(IngestCsvFileCommand command) {
		return switch (command.fileType()) {
			case SALES -> ingestSales(command);
			case INVENTORY -> ingestInventory(command);
		};
	}

	private IngestCsvFileResult ingestSales(IngestCsvFileCommand command) {
		List<RowRejection> rejections = new ArrayList<>(command.preValidationRejections());
		List<Sale> accepted = new ArrayList<>();
		Set<String> seenInThisBatch = new HashSet<>();

		for (CandidateRow candidate : command.candidateRows()) {
			Sale sale = candidate.sale();

			if (productRepository.findById(sale.productId()).isEmpty()) {
				rejections.add(new RowRejection(candidate.rowNumber(),
						"producto_id " + sale.productId() + " no existe en el catálogo"));
				continue;
			}
			if (storeRepository.findById(sale.storeId()).isEmpty()) {
				rejections.add(new RowRejection(candidate.rowNumber(), "sucursal_id " + sale.storeId() + " no existe"));
				continue;
			}

			String duplicateKey = sale.productId() + "|" + sale.storeId() + "|" + sale.date();
			if (saleIngestionRepository.existsByProductStoreAndDate(sale.productId(), sale.storeId(), sale.date())) {
				rejections.add(new RowRejection(candidate.rowNumber(),
						"ya existe una venta para el producto " + sale.productId() + ", sucursal " + sale.storeId()
								+ " y fecha " + sale.date()));
				continue;
			}
			if (!seenInThisBatch.add(duplicateKey)) {
				rejections.add(new RowRejection(candidate.rowNumber(),
						"fila duplicada dentro del mismo archivo para el producto " + sale.productId()
								+ ", sucursal " + sale.storeId() + " y fecha " + sale.date()));
				continue;
			}

			accepted.add(sale);
		}

		checkRejectionThreshold(command.totalRowsRead(), rejections);

		for (Sale sale : accepted) {
			saleIngestionRepository.save(sale);
		}

		return new IngestCsvFileResult(command.totalRowsRead(), accepted.size(), rejections.size(), rejections);
	}

	private IngestCsvFileResult ingestInventory(IngestCsvFileCommand command) {
		List<RowRejection> rejections = new ArrayList<>(command.preValidationRejections());
		List<Inventory> accepted = new ArrayList<>();
		Set<String> seenInThisBatch = new HashSet<>();

		for (InventoryCandidateRow candidate : command.inventoryCandidateRows()) {
			Inventory snapshot = candidate.inventory();

			if (productRepository.findById(snapshot.productId()).isEmpty()) {
				rejections.add(new RowRejection(candidate.rowNumber(),
						"producto_id " + snapshot.productId() + " no existe en el catálogo"));
				continue;
			}
			if (storeRepository.findById(snapshot.storeId()).isEmpty()) {
				rejections.add(new RowRejection(candidate.rowNumber(),
						"sucursal_id " + snapshot.storeId() + " no existe"));
				continue;
			}

			String duplicateKey = snapshot.productId() + "|" + snapshot.storeId() + "|" + snapshot.snapshotDate();
			if (inventoryIngestionRepository.existsByProductStoreAndSnapshotDate(
					snapshot.productId(), snapshot.storeId(), snapshot.snapshotDate())) {
				rejections.add(new RowRejection(candidate.rowNumber(),
						"ya existe un snapshot de inventario para el producto " + snapshot.productId()
								+ ", sucursal " + snapshot.storeId() + " y fecha " + snapshot.snapshotDate()));
				continue;
			}
			if (!seenInThisBatch.add(duplicateKey)) {
				rejections.add(new RowRejection(candidate.rowNumber(),
						"fila duplicada dentro del mismo archivo para el producto " + snapshot.productId()
								+ ", sucursal " + snapshot.storeId() + " y fecha " + snapshot.snapshotDate()));
				continue;
			}

			accepted.add(snapshot);
		}

		checkRejectionThreshold(command.totalRowsRead(), rejections);

		for (Inventory snapshot : accepted) {
			inventoryIngestionRepository.save(snapshot.snapshotDate(), snapshot.productId(), snapshot.storeId(),
					snapshot.currentStock(), snapshot.stockInTransit());
		}

		return new IngestCsvFileResult(command.totalRowsRead(), accepted.size(), rejections.size(), rejections);
	}

	/** Todo-o-nada por lote (Sección 7.4): corta antes de persistir nada si se supera el umbral. */
	private static void checkRejectionThreshold(int totalRowsRead, List<RowRejection> rejections) {
		double rejectionRate = totalRowsRead == 0
				? 0.0
				: (rejections.size() * 100.0) / totalRowsRead;

		if (rejectionRate > REJECTION_THRESHOLD_PERCENT) {
			throw new CsvIngestionThresholdExceededException(
					totalRowsRead, rejections.size(), rejectionRate, rejections);
		}
	}
}

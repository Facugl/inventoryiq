package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CandidateRow;
import com.inventoryiq.application.port.in.IngestCsvFileCommand;
import com.inventoryiq.application.port.in.IngestCsvFileResult;
import com.inventoryiq.application.port.in.IngestCsvFileUseCase;
import com.inventoryiq.application.port.in.RowRejection;
import com.inventoryiq.application.port.out.ProductRepository;
import com.inventoryiq.application.port.out.SaleIngestionRepository;
import com.inventoryiq.application.port.out.StoreRepository;
import com.inventoryiq.domain.exception.CsvIngestionThresholdExceededException;
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
 * (SaleIngestionRepository.existsByProductStoreAndDate) y entre filas
 * del propio archivo subido (dos filas del mismo producto+sucursal+
 * fecha en un mismo lote) — lo segundo no lo detectaría el repositorio
 * por sí solo, porque nada se persiste hasta el final de la corrida.
 */
public class IngestCsvFileService implements IngestCsvFileUseCase {

	private static final double REJECTION_THRESHOLD_PERCENT = 5.0;

	private final ProductRepository productRepository;
	private final StoreRepository storeRepository;
	private final SaleIngestionRepository saleIngestionRepository;

	public IngestCsvFileService(
			ProductRepository productRepository, StoreRepository storeRepository, SaleIngestionRepository saleIngestionRepository) {
		this.productRepository = productRepository;
		this.storeRepository = storeRepository;
		this.saleIngestionRepository = saleIngestionRepository;
	}

	@Override
	public IngestCsvFileResult execute(IngestCsvFileCommand command) {
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

		double rejectionRate = command.totalRowsRead() == 0
				? 0.0
				: (rejections.size() * 100.0) / command.totalRowsRead();

		if (rejectionRate > REJECTION_THRESHOLD_PERCENT) {
			throw new CsvIngestionThresholdExceededException(
					command.totalRowsRead(), rejections.size(), rejectionRate, rejections);
		}

		for (Sale sale : accepted) {
			saleIngestionRepository.save(sale);
		}

		return new IngestCsvFileResult(command.totalRowsRead(), accepted.size(), rejections.size(), rejections);
	}
}

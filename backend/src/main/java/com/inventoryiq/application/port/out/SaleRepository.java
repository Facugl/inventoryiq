package com.inventoryiq.application.port.out;

import com.inventoryiq.domain.model.Sale;

import java.time.LocalDate;
import java.util.List;

/** Puerto de salida — Sección 2.3.2. Rango de fechas inclusivo en ambos extremos. */
public interface SaleRepository {

	List<Sale> findByProductAndStore(Long productId, Long storeId, LocalDate from, LocalDate to);
}

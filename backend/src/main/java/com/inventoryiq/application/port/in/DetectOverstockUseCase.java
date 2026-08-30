package com.inventoryiq.application.port.in;

import java.util.List;

/**
 * Puerto de entrada — Sección 2.3.2 / 9.3. Devuelve los productos en
 * estado Sobrestock del scope solicitado, ordenados según sortBy.
 */
public interface DetectOverstockUseCase {

	List<OverstockProductResult> execute(DetectOverstockQuery query);
}

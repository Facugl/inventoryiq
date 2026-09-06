package com.inventoryiq.application.port.in;

import java.util.List;

/** Puerto de entrada. Busca en el catálogo activo por código (sku) o nombre, sin distinguir mayúsculas/acentos exactos. */
public interface SearchProductsUseCase {

	List<ProductSummaryResult> execute(SearchProductsQuery query);
}

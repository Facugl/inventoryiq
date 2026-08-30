package com.inventoryiq.application.port.in;

import java.util.List;

/**
 * Puerto de entrada — Sección 2.3.2 / 9.1. Devuelve los productos en
 * estado Crítico o Requiere Reposición del scope solicitado, ordenados
 * por score de criticidad descendente.
 */
public interface GetCriticalProductsUseCase {

	List<CriticalProductResult> execute(GetCriticalProductsQuery query);
}

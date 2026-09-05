package com.inventoryiq.application.port.in;

import java.util.List;

/**
 * Puerto de entrada. Lista las sucursales activas — hasta ahora
 * StoreRepository.findAllActive() solo se usaba internamente (Sección
 * 9.10); este caso de uso es el primero en exponerlo a un cliente externo,
 * para que el frontend deje de asumir un catálogo fijo de sucursales.
 */
public interface ListStoresUseCase {

	List<StoreResult> execute();
}

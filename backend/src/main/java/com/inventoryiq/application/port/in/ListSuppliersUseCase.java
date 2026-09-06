package com.inventoryiq.application.port.in;

import java.util.List;

/**
 * Puerto de entrada. Lista los proveedores activos — sin fuente automática
 * (Sección 8.11): el catálogo de proveedores se carga y mantiene a mano
 * porque la coordinación real con proveedores ocurre por WhatsApp, sin
 * ningún sistema del que importarla.
 */
public interface ListSuppliersUseCase {

	List<SupplierResult> execute();
}

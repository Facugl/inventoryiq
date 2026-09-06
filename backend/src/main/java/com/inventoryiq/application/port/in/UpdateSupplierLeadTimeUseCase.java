package com.inventoryiq.application.port.in;

/**
 * Puerto de entrada. Corrige el lead time promedio de un proveedor —
 * información que hoy no existe en ningún sistema (Sección 8.11) y que
 * solo el usuario conoce por experiencia, cargada y ajustada a mano.
 */
public interface UpdateSupplierLeadTimeUseCase {

	SupplierResult execute(UpdateSupplierLeadTimeCommand command);
}

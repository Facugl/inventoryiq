package com.inventoryiq.application.port.in;

/**
 * Puerto de entrada (Sección 8.10). Corrige los parámetros de negocio de
 * una categoría — hasta ahora solo editables a mano en categorias.csv,
 * con reinicio del backend incluido.
 */
public interface UpdateCategoryParametersUseCase {

	CategoryResult execute(UpdateCategoryParametersCommand command);
}

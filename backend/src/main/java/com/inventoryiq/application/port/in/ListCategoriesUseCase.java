package com.inventoryiq.application.port.in;

import java.util.List;

/**
 * Puerto de entrada. Lista el catálogo completo de categorías —
 * CategoryRepository solo se usaba internamente para resolver una
 * categoría puntual (Sección 5.2); este caso de uso es el primero en
 * exponer el catálogo completo a un cliente externo.
 */
public interface ListCategoriesUseCase {

	List<CategoryResult> execute();
}

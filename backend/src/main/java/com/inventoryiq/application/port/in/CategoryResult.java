package com.inventoryiq.application.port.in;

/** Salida de ListCategoriesUseCase — solo lo que un lookup de categoría necesita. */
public record CategoryResult(Long categoryId, String name, Long parentCategoryId) {
}

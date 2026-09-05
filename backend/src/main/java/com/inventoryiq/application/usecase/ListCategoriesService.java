package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CategoryResult;
import com.inventoryiq.application.port.in.ListCategoriesUseCase;
import com.inventoryiq.application.port.out.CategoryRepository;

import java.util.List;

/** Implementación de ListCategoriesUseCase. Pura traducción: sin reglas de negocio propias. */
public class ListCategoriesService implements ListCategoriesUseCase {

	private final CategoryRepository categoryRepository;

	public ListCategoriesService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Override
	public List<CategoryResult> execute() {
		return categoryRepository.findAll().stream()
				.map(category -> new CategoryResult(category.categoryId(), category.name(), category.parentCategoryId()))
				.toList();
	}
}

package com.inventoryiq.application.usecase;

import com.inventoryiq.application.port.in.CategoryResult;
import com.inventoryiq.application.port.in.UpdateCategoryParametersCommand;
import com.inventoryiq.application.port.in.UpdateCategoryParametersUseCase;
import com.inventoryiq.application.port.out.CategoryRepository;
import com.inventoryiq.domain.exception.CategoryNotFoundException;
import com.inventoryiq.domain.model.Category;

/** Implementación de UpdateCategoryParametersUseCase. */
public class UpdateCategoryParametersService implements UpdateCategoryParametersUseCase {

	private final CategoryRepository categoryRepository;

	public UpdateCategoryParametersService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Override
	public CategoryResult execute(UpdateCategoryParametersCommand command) {
		categoryRepository.findById(command.categoryId())
				.orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

		Category updated = categoryRepository.updateParameters(
				command.categoryId(), command.maxCoverageDaysThreshold(), command.defaultExtraCoverageDays());

		return new CategoryResult(
				updated.categoryId(), updated.name(), updated.parentCategoryId(),
				updated.maxCoverageDaysThreshold(), updated.defaultExtraCoverageDays());
	}
}

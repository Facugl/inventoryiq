package com.inventoryiq.domain.exception;

/** Se lanza cuando se busca una categoría por id y no existe. */
public class CategoryNotFoundException extends NotFoundException {
	public CategoryNotFoundException(Long categoryId) {
		super(ErrorCode.CATEGORY_NOT_FOUND, categoryId, "Category not found: " + categoryId);
	}
}

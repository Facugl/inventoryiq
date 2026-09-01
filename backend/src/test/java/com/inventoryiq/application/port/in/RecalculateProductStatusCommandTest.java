package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecalculateProductStatusCommandTest {

	@Test
	void referenceDateIsRequired() {
		assertThrows(InvalidDomainDataException.class, () -> new RecalculateProductStatusCommand(1L, null));
	}

	@Test
	void storeIdIsOptional() {
		assertDoesNotThrow(() -> new RecalculateProductStatusCommand(null, LocalDate.now()));
	}
}

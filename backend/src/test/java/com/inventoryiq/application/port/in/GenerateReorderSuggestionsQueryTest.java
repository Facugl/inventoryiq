package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GenerateReorderSuggestionsQueryTest {

	@Test
	void storeIdIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new GenerateReorderSuggestionsQuery(null, null, null, LocalDate.now()));
	}

	@Test
	void referenceDateIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new GenerateReorderSuggestionsQuery(1L, null, null, null));
	}
}

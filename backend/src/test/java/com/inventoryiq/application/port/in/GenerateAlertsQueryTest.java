package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GenerateAlertsQueryTest {

	@Test
	void storeIdIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new GenerateAlertsQuery(null, LocalDate.now(), null, null));
	}

	@Test
	void referenceDateIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new GenerateAlertsQuery(1L, null, null, null));
	}
}

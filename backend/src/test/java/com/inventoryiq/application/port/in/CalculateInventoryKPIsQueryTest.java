package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculateInventoryKPIsQueryTest {

	@Test
	void storeIdIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new CalculateInventoryKPIsQuery(null, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-01")));
	}

	@Test
	void fromDateIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new CalculateInventoryKPIsQuery(1L, null, LocalDate.parse("2026-08-01")));
	}

	@Test
	void toDateIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new CalculateInventoryKPIsQuery(1L, LocalDate.parse("2026-07-01"), null));
	}

	@Test
	void fromDateCannotBeAfterToDate() {
		assertThrows(InvalidDomainDataException.class,
				() -> new CalculateInventoryKPIsQuery(1L, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-07-01")));
	}
}

package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DetectOverstockQueryTest {

	@Test
	void storeIdIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> DetectOverstockQuery.of(null, null, LocalDate.now(), OverstockSortBy.IMMOBILIZED_VALUE));
	}

	@Test
	void referenceDateIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> DetectOverstockQuery.of(1L, null, null, OverstockSortBy.IMMOBILIZED_VALUE));
	}

	@Test
	void sortByIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> DetectOverstockQuery.of(1L, null, LocalDate.now(), null));
	}

	@Test
	void salesWindowDaysMustBePositive() {
		assertThrows(InvalidDomainDataException.class,
				() -> new DetectOverstockQuery(1L, null, LocalDate.now(), 0, OverstockSortBy.IMMOBILIZED_VALUE));
	}
}

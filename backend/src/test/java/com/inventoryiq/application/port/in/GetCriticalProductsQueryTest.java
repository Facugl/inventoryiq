package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GetCriticalProductsQueryTest {

	@Test
	void storeIdIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> GetCriticalProductsQuery.of(null, null, null, LocalDate.now()));
	}

	@Test
	void referenceDateIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> GetCriticalProductsQuery.of(1L, null, null, null));
	}

	@Test
	void salesWindowDaysMustBePositive() {
		assertThrows(InvalidDomainDataException.class,
				() -> new GetCriticalProductsQuery(1L, null, null, LocalDate.now(), 0));
	}

	@Test
	void limitMustBePositiveWhenProvided() {
		assertThrows(InvalidDomainDataException.class,
				() -> GetCriticalProductsQuery.of(1L, null, 0, LocalDate.now()));
		assertThrows(InvalidDomainDataException.class,
				() -> GetCriticalProductsQuery.of(1L, null, -1, LocalDate.now()));
	}
}

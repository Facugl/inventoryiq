package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ForecastDemandQueryTest {

	@Test
	void productIdIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new ForecastDemandQuery(null, 1L, LocalDate.now(), 30));
	}

	@Test
	void storeIdIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new ForecastDemandQuery(1L, null, LocalDate.now(), 30));
	}

	@Test
	void referenceDateIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> new ForecastDemandQuery(1L, 1L, null, 30));
	}

	@Test
	void horizonDaysMustBePositive() {
		assertThrows(InvalidDomainDataException.class,
				() -> new ForecastDemandQuery(1L, 1L, LocalDate.now(), 0));
	}
}

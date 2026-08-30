package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassifyProductsQueryTest {

	@Test
	void storeIdIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> ClassifyProductsQuery.of(null, null, LocalDate.now()));
	}

	@Test
	void referenceDateIsRequired() {
		assertThrows(InvalidDomainDataException.class,
				() -> ClassifyProductsQuery.of(1L, null, null));
	}

	@Test
	void analysisWindowDaysMustBePositive() {
		assertThrows(InvalidDomainDataException.class,
				() -> new ClassifyProductsQuery(1L, null, LocalDate.now(), 0));
	}

	@Test
	void ofDefaultsAnalysisWindowDaysTo365() {
		ClassifyProductsQuery query = ClassifyProductsQuery.of(1L, null, LocalDate.now());
		assertEquals(365, query.analysisWindowDays());
	}
}

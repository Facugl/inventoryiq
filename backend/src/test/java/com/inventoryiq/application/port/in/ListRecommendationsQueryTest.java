package com.inventoryiq.application.port.in;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ListRecommendationsQueryTest {

	@Test
	void storeIdIsRequired() {
		assertThrows(InvalidDomainDataException.class, () -> new ListRecommendationsQuery(null, null, null, null));
	}
}

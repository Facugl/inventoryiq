package com.inventoryiq.domain.service;

import com.inventoryiq.domain.model.XyzClassification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XyzClassifierTest {

	@Test
	void cvLessThan05IsClassX() {
		assertEquals(XyzClassification.X, XyzClassifier.classify(10, 3)); // CV = 0.3
	}

	@Test
	void cvBetween05And1IsClassY() {
		assertEquals(XyzClassification.Y, XyzClassifier.classify(10, 7)); // CV = 0.7
	}

	@Test
	void cvGreaterThanOrEqualTo1IsClassZ() {
		assertEquals(XyzClassification.Z, XyzClassifier.classify(10, 12)); // CV = 1.2
	}

	@Test
	void theExactLimitOf05IsClassY() {
		assertEquals(XyzClassification.Y, XyzClassifier.classify(10, 5)); // CV = 0.5 exacto
	}
}
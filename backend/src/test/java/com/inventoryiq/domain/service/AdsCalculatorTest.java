package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.vo.DailySalesRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdsCalculatorTest {

	@Test
	void calculatesSimpleAdsAsTheAverageOfSales() {
		double ads = AdsCalculator.calculateSimpleAds(List.of(10, 20, 30));
		assertEquals(20.0, ads, 0.001);
	}

	@Test
	void rejectsEmptyListForSimpleAds() {
		assertThrows(InvalidDomainDataException.class,
				() -> AdsCalculator.calculateSimpleAds(List.of()));
	}

	@Test
	void correctedAdsExcludesDaysWithStockout() {
		List<DailySalesRecord> records = List.of(
				new DailySalesRecord(LocalDate.of(2026, 1, 1), 10, 5),
				new DailySalesRecord(LocalDate.of(2026, 1, 2), 0, 0), // quiebre: se excluye
				new DailySalesRecord(LocalDate.of(2026, 1, 3), 20, 15));
		// (10 + 20) / 2 días sin quiebre = 15, NO (10+0+20)/3 = 10
		assertEquals(15.0, AdsCalculator.calculateCorrectedAds(records), 0.001);
	}

	@Test
	void rejectsCalculatingCorrectedAdsIfAllDaysHadStockout() {
		List<DailySalesRecord> records = List.of(
				new DailySalesRecord(LocalDate.of(2026, 1, 1), 0, 0),
				new DailySalesRecord(LocalDate.of(2026, 1, 2), 0, 0));
		assertThrows(InvalidDomainDataException.class,
				() -> AdsCalculator.calculateCorrectedAds(records));
	}
}
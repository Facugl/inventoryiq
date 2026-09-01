package com.inventoryiq.domain.service;

import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.model.vo.DailySalesRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SeasonalityCalculatorTest {

	@Test
	void aPeakMonthGetsAnIndexAboveOneAndALowMonthBelowOne() {
		List<DailySalesRecord> records = new ArrayList<>();
		addDays(records, LocalDate.parse("2025-11-01"), 5, 10, 5); // noviembre: 10 u/día
		addDays(records, LocalDate.parse("2025-12-01"), 5, 20, 5); // diciembre: 20 u/día

		// ADS del período = (5*10 + 5*20) / 10 = 15
		double decemberIndex = SeasonalityCalculator.calculateSeasonalIndex(records, YearMonth.of(2025, 12));
		double novemberIndex = SeasonalityCalculator.calculateSeasonalIndex(records, YearMonth.of(2025, 11));

		assertEquals(20.0 / 15.0, decemberIndex, 1e-9);
		assertEquals(10.0 / 15.0, novemberIndex, 1e-9);
	}

	@Test
	void excludesStockoutDaysFromBothTheMonthlyAndThePeriodAverage() {
		List<DailySalesRecord> records = new ArrayList<>();
		addDays(records, LocalDate.parse("2025-11-01"), 5, 10, 5);
		addDays(records, LocalDate.parse("2025-12-01"), 5, 20, 5);
		records.add(new DailySalesRecord(LocalDate.parse("2025-12-06"), 999, 0)); // quiebre: se descarta

		double decemberIndex = SeasonalityCalculator.calculateSeasonalIndex(records, YearMonth.of(2025, 12));

		assertEquals(20.0 / 15.0, decemberIndex, 1e-9);
	}

	@Test
	void throwsWhenTheTargetMonthHasNoHistoryInThePeriod() {
		List<DailySalesRecord> records = new ArrayList<>();
		addDays(records, LocalDate.parse("2025-11-01"), 5, 10, 5);

		assertThrows(InvalidDomainDataException.class,
				() -> SeasonalityCalculator.calculateSeasonalIndex(records, YearMonth.of(2026, 1)));
	}

	@Test
	void throwsWhenEveryDayInThePeriodHadStockout() {
		List<DailySalesRecord> records = List.of(
				new DailySalesRecord(LocalDate.parse("2025-11-01"), 10, 0),
				new DailySalesRecord(LocalDate.parse("2025-11-02"), 10, 0));

		assertThrows(InvalidDomainDataException.class,
				() -> SeasonalityCalculator.calculateSeasonalIndex(records, YearMonth.of(2025, 11)));
	}

	@Test
	void throwsWhenThePeriodAdsIsZero() {
		List<DailySalesRecord> records = new ArrayList<>();
		addDays(records, LocalDate.parse("2025-11-01"), 5, 0, 5);

		assertThrows(InvalidDomainDataException.class,
				() -> SeasonalityCalculator.calculateSeasonalIndex(records, YearMonth.of(2025, 11)));
	}

	private static void addDays(List<DailySalesRecord> records, LocalDate start, int count, int unitsSold, int stockAtStartOfDay) {
		for (int i = 0; i < count; i++) {
			records.add(new DailySalesRecord(start.plusDays(i), unitsSold, stockAtStartOfDay));
		}
	}
}

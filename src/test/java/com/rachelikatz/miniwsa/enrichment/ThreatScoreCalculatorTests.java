package com.rachelikatz.miniwsa.enrichment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.Severity;

class ThreatScoreCalculatorTests {

	private final ThreatScoreCalculator calculator = new ThreatScoreCalculator();

	@ParameterizedTest
	@CsvSource({
			"CRITICAL, 40",
			"HIGH, 30",
			"MEDIUM, 20",
			"LOW, 10"
	})
	void appliesSeverityScore(Severity severity, int expectedScore) {
		assertThat(calculator.calculate(severity, EventAction.MONITOR, "/public", false))
				.isEqualTo(expectedScore);
	}

	@ParameterizedTest
	@CsvSource({
			"DENY, 20",
			"ALERT, 10",
			"MONITOR, 0"
	})
	void appliesActionScore(EventAction action, int expectedActionScore) {
		assertThat(calculator.calculate(Severity.LOW, action, "/public", false))
				.isEqualTo(10 + expectedActionScore);
	}

	@Test
	void addsAdminPathBonus() {
		assertThat(calculator.calculate(Severity.LOW, EventAction.MONITOR, "/admin/users", false))
				.isEqualTo(25);
	}

	@Test
	void addsLoginPathBonus() {
		assertThat(calculator.calculate(Severity.LOW, EventAction.MONITOR, "/api/login", false))
				.isEqualTo(25);
	}

	@Test
	void doesNotAddPathBonusForOrdinaryPath() {
		assertThat(calculator.calculate(Severity.LOW, EventAction.MONITOR, "/api/orders", false))
				.isEqualTo(10);
	}

	@Test
	void matchesSensitivePathsCaseSensitively() {
		assertThat(calculator.calculate(Severity.LOW, EventAction.MONITOR, "/Admin/users", false))
				.isEqualTo(10);
	}

	@Test
	void addsRepeatOffenderBonusWhenTrue() {
		assertThat(calculator.calculate(Severity.LOW, EventAction.MONITOR, "/public", true))
				.isEqualTo(25);
	}

	@Test
	void doesNotAddRepeatOffenderBonusWhenFalse() {
		assertThat(calculator.calculate(Severity.LOW, EventAction.MONITOR, "/public", false))
				.isEqualTo(10);
	}

	@Test
	void combinesScoreComponents() {
		assertThat(calculator.calculate(Severity.HIGH, EventAction.ALERT, "/api/login", true))
				.isEqualTo(70);
	}

	@Test
	void maximumConfiguredCombinationDoesNotExceedCap() {
		int score = calculator.calculate(
				Severity.CRITICAL,
				EventAction.DENY,
				"/admin/login",
				true);

		assertThat(score).isEqualTo(90);
		assertThat(score).isLessThanOrEqualTo(100);
	}
}

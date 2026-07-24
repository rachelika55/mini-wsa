package com.rachelikatz.miniwsa.enrichment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.rachelikatz.miniwsa.domain.AttackType;
import com.rachelikatz.miniwsa.domain.RuleCategory;

class AttackClassifierTests {

	private final AttackClassifier classifier = new AttackClassifier();

	@ParameterizedTest
	@MethodSource("categoryMappings")
	void mapsEveryCategory(
			RuleCategory category,
			AttackType expectedAttackType,
			String expectedDisplayValue) {
		AttackType attackType = classifier.classify(category);

		assertThat(attackType).isEqualTo(expectedAttackType);
		assertThat(attackType.displayValue()).isEqualTo(expectedDisplayValue);
	}

	private static Stream<Arguments> categoryMappings() {
		return Stream.of(
				Arguments.of(
						RuleCategory.INJECTION,
						AttackType.SQL_COMMAND_INJECTION,
						"SQL/Command Injection"),
				Arguments.of(
						RuleCategory.XSS,
						AttackType.CROSS_SITE_SCRIPTING,
						"Cross-Site Scripting"),
				Arguments.of(
						RuleCategory.PROTOCOL_VIOLATION,
						AttackType.PROTOCOL_ANOMALY,
						"Protocol Anomaly"),
				Arguments.of(
						RuleCategory.DATA_LEAKAGE,
						AttackType.DATA_EXFILTRATION,
						"Data Exfiltration"),
				Arguments.of(
						RuleCategory.BOT,
						AttackType.BOT_ACTIVITY,
						"Bot Activity"),
				Arguments.of(
						RuleCategory.DOS,
						AttackType.DENIAL_OF_SERVICE,
						"Denial of Service"),
				Arguments.of(
						RuleCategory.RATE_LIMIT,
						AttackType.RATE_LIMITING,
						"Rate Limiting"));
	}
}

package com.rachelikatz.miniwsa.enrichment;

import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.Severity;

public class ThreatScoreCalculator {

	private static final int SENSITIVE_PATH_BONUS = 15;
	private static final int REPEAT_OFFENDER_BONUS = 15;
	private static final int MAXIMUM_SCORE = 100;

	public int calculate(
			Severity severity,
			EventAction action,
			String path,
			boolean repeatOffender) {
		int score = severityScore(severity) + actionScore(action);

		if (path.contains("/admin") || path.contains("/login")) {
			score += SENSITIVE_PATH_BONUS;
		}

		if (repeatOffender) {
			score += REPEAT_OFFENDER_BONUS;
		}

		return Math.min(score, MAXIMUM_SCORE);
	}

	private int severityScore(Severity severity) {
		return switch (severity) {
			case CRITICAL -> 40;
			case HIGH -> 30;
			case MEDIUM -> 20;
			case LOW -> 10;
		};
	}

	private int actionScore(EventAction action) {
		return switch (action) {
			case DENY -> 20;
			case ALERT -> 10;
			case MONITOR -> 0;
		};
	}
}

package com.rachelikatz.miniwsa.persistence.projection;

import com.rachelikatz.miniwsa.domain.RuleCategory;

public record CategoryAggregate(
		RuleCategory category,
		long count,
		double avgThreatScore) {
}

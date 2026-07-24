package com.rachelikatz.miniwsa.persistence.repository;

import java.time.Instant;

import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.RuleCategory;

/**
 * Fully-validated inputs for a samples query. All filter fields are optional
 * (null means "no constraint"); {@code from} is inclusive and {@code to} is
 * exclusive. {@code limit} and {@code offset} are already range-checked.
 */
public record SampleFilter(
		Long configId,
		Instant from,
		Instant to,
		RuleCategory category,
		EventAction action,
		int limit,
		int offset) {
}

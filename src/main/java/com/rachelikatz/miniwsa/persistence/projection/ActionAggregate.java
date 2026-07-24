package com.rachelikatz.miniwsa.persistence.projection;

import com.rachelikatz.miniwsa.domain.EventAction;

public record ActionAggregate(
		EventAction action,
		long count) {
}

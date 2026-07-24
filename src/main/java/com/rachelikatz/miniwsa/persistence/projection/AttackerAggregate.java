package com.rachelikatz.miniwsa.persistence.projection;

public record AttackerAggregate(
		String clientIp,
		long count,
		double avgThreatScore) {
}

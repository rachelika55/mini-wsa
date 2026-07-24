package com.rachelikatz.miniwsa.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SummaryResponse(
		Long configId,
		TimeRange timeRange,
		long totalEvents,
		Map<String, CategoryStats> byCategory,
		Map<String, Long> byAction,
		List<AttackerStats> topAttackers,
		List<PathStats> topTargetedPaths) {

	public record TimeRange(Instant from, Instant to) {
	}

	public record CategoryStats(long count, double avgThreatScore) {
	}

	public record AttackerStats(String clientIp, long count, double avgThreatScore) {
	}

	public record PathStats(String path, long count) {
	}
}

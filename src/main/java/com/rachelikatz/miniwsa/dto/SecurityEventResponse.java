package com.rachelikatz.miniwsa.dto;

import java.time.Instant;

import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.domain.Severity;

public record SecurityEventResponse(
		String eventId,
		Instant timestamp,
		long configId,
		String policyId,
		String clientIp,
		String hostname,
		String path,
		String method,
		int statusCode,
		String userAgent,
		RuleResponse rule,
		EventAction action,
		GeoLocationResponse geoLocation,
		long requestSize,
		long responseSize,
		String attackType,
		int threatScore,
		Instant receivedAt) {

	public record RuleResponse(
			String id,
			String name,
			String message,
			Severity severity,
			RuleCategory category) {
	}

	public record GeoLocationResponse(
			String country,
			String city) {
	}
}

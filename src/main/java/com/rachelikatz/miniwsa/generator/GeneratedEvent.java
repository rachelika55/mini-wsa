package com.rachelikatz.miniwsa.generator;

/**
 * A generated security event shaped exactly like the ingestion API request body.
 * Enum-valued fields and the timestamp are emitted as strings so the JSON can be
 * POSTed to {@code /v1/events/ingest} verbatim, with no serializer configuration.
 */
public record GeneratedEvent(
		String eventId,
		String timestamp,
		long configId,
		String policyId,
		String clientIp,
		String hostname,
		String path,
		String method,
		int statusCode,
		String userAgent,
		GeneratedRule rule,
		String action,
		GeneratedGeoLocation geoLocation,
		long requestSize,
		long responseSize) {

	public record GeneratedRule(
			String id,
			String name,
			String message,
			String severity,
			String category) {
	}

	public record GeneratedGeoLocation(
			String country,
			String city) {
	}
}

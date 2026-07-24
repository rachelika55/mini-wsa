package com.rachelikatz.miniwsa.dto;

import java.time.Instant;

import com.rachelikatz.miniwsa.domain.EventAction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SecurityEventRequest(
		@NotBlank String eventId,
		@NotNull Instant timestamp,
		@NotNull Long configId,
		@NotBlank String policyId,
		@NotBlank String clientIp,
		@NotBlank String hostname,
		@NotBlank String path,
		@NotBlank String method,
		@NotNull Integer statusCode,
		@NotBlank String userAgent,
		@NotNull @Valid SecurityRuleRequest rule,
		@NotNull EventAction action,
		@NotNull @Valid GeoLocationRequest geoLocation,
		@NotNull Long requestSize,
		@NotNull Long responseSize) {
}

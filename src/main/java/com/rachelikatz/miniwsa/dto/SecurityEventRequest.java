package com.rachelikatz.miniwsa.dto;

import java.time.Instant;

import com.rachelikatz.miniwsa.domain.EventAction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SecurityEventRequest(
		@NotBlank @Size(max = 255) String eventId,
		@NotNull Instant timestamp,
		@NotNull @Positive Long configId,
		@NotBlank @Size(max = 255) String policyId,
		@NotBlank @Size(max = 255) String clientIp,
		@NotBlank @Size(max = 255) String hostname,
		@NotBlank @Size(max = 2048) String path,
		@NotBlank @Size(max = 255) String method,
		@NotNull @Min(100) @Max(599) Integer statusCode,
		@NotBlank @Size(max = 2048) String userAgent,
		@NotNull @Valid SecurityRuleRequest rule,
		@NotNull EventAction action,
		@NotNull @Valid GeoLocationRequest geoLocation,
		@NotNull @PositiveOrZero Long requestSize,
		@NotNull @PositiveOrZero Long responseSize) {
}

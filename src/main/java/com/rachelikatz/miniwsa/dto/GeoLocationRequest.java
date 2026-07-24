package com.rachelikatz.miniwsa.dto;

import jakarta.validation.constraints.NotBlank;

public record GeoLocationRequest(
		@NotBlank String country,
		@NotBlank String city) {
}

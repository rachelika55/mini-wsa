package com.rachelikatz.miniwsa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeoLocationRequest(
		@NotBlank @Size(max = 255) String country,
		@NotBlank @Size(max = 255) String city) {
}

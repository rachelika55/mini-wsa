package com.rachelikatz.miniwsa.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		List<FieldViolation> violations) {

	public static ApiError of(
			Instant timestamp,
			int status,
			String error,
			String message,
			String path) {
		return new ApiError(timestamp, status, error, message, path, List.of());
	}
}

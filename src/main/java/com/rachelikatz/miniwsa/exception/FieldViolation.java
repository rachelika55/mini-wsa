package com.rachelikatz.miniwsa.exception;

public record FieldViolation(
		String field,
		String message) {
}

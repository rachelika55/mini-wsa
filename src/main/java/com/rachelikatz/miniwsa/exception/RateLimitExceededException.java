package com.rachelikatz.miniwsa.exception;

/** Thrown when a client exceeds the configured per-IP request rate. */
public class RateLimitExceededException extends RuntimeException {

	public RateLimitExceededException(String message) {
		super(message);
	}
}

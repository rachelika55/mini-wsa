package com.rachelikatz.miniwsa.exception;

/** Thrown when an ingestion batch exceeds the configured maximum size. */
public class PayloadTooLargeException extends RuntimeException {

	public PayloadTooLargeException(String message) {
		super(message);
	}
}

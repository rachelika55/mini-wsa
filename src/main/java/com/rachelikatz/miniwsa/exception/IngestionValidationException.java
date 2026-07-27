package com.rachelikatz.miniwsa.exception;

import java.util.List;

public class IngestionValidationException extends RuntimeException {

	private final transient List<FieldViolation> violations;

	public IngestionValidationException(List<FieldViolation> violations) {
		super("Validation failed for one or more events");
		this.violations = List.copyOf(violations);
	}

	public List<FieldViolation> getViolations() {
		return violations;
	}
}

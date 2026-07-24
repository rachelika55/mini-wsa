package com.rachelikatz.miniwsa.exception;

import java.util.List;

public class IngestionValidationException extends RuntimeException {

	private final transient List<FieldViolation> violations;

	public IngestionValidationException(List<FieldViolation> violations) {
		super("Ingestion request failed validation");
		this.violations = List.copyOf(violations);
	}

	public List<FieldViolation> getViolations() {
		return violations;
	}
}

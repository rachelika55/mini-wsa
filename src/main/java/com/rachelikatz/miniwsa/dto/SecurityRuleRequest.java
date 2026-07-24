package com.rachelikatz.miniwsa.dto;

import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.domain.Severity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SecurityRuleRequest(
		@NotBlank String id,
		@NotBlank String name,
		@NotBlank String message,
		@NotNull Severity severity,
		@NotNull RuleCategory category) {
}

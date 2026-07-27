package com.rachelikatz.miniwsa.dto;

import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.domain.Severity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SecurityRuleRequest(
		@NotBlank @Size(max = 255) String id,
		@NotBlank @Size(max = 255) String name,
		@NotBlank @Size(max = 2048) String message,
		@NotNull Severity severity,
		@NotNull RuleCategory category) {
}

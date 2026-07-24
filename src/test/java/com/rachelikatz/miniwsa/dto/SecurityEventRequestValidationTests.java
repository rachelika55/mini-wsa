package com.rachelikatz.miniwsa.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.domain.Severity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SecurityEventRequestValidationTests {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void completeRequestIsValid() {
		Set<ConstraintViolation<SecurityEventRequest>> violations = validator.validate(validRequest());

		assertThat(violations).isEmpty();
	}

	@Test
	void missingRequiredTopLevelFieldIsInvalid() {
		SecurityEventRequest request = new SecurityEventRequest(
				null,
				Instant.parse("2026-05-20T14:32:10Z"),
				14227L,
				"pol_web1",
				"203.0.113.42",
				"www.example.com",
				"/api/v1/login",
				"POST",
				403,
				"Mozilla/5.0",
				validRule(),
				EventAction.DENY,
				validGeoLocation(),
				1024L,
				256L);

		Set<ConstraintViolation<SecurityEventRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anyMatch(violation -> violation.getPropertyPath().toString().equals("eventId"));
	}

	@Test
	void blankRequiredStringFieldIsInvalid() {
		SecurityEventRequest request = new SecurityEventRequest(
				" ",
				Instant.parse("2026-05-20T14:32:10Z"),
				14227L,
				"pol_web1",
				"203.0.113.42",
				"www.example.com",
				"/api/v1/login",
				"POST",
				403,
				"Mozilla/5.0",
				validRule(),
				EventAction.DENY,
				validGeoLocation(),
				1024L,
				256L);

		Set<ConstraintViolation<SecurityEventRequest>> violations = validator.validate(request);

		assertThat(violations)
				.anyMatch(violation -> violation.getPropertyPath().toString().equals("eventId"));
	}

	@Test
	void missingNestedRuleFieldIsInvalid() {
		SecurityRuleRequest rule = new SecurityRuleRequest(
				"950001",
				"SQL_INJECTION",
				"SQL Injection Attack Detected",
				Severity.CRITICAL,
				null);

		Set<ConstraintViolation<SecurityEventRequest>> violations =
				validator.validate(validRequest(rule, validGeoLocation()));

		assertThat(violations)
				.anyMatch(violation -> violation.getPropertyPath().toString().equals("rule.category"));
	}

	@Test
	void missingNestedGeoLocationFieldIsInvalid() {
		GeoLocationRequest geoLocation = new GeoLocationRequest("CN", null);

		Set<ConstraintViolation<SecurityEventRequest>> violations =
				validator.validate(validRequest(validRule(), geoLocation));

		assertThat(violations)
				.anyMatch(violation -> violation.getPropertyPath().toString().equals("geoLocation.city"));
	}

	private SecurityEventRequest validRequest() {
		return validRequest(validRule(), validGeoLocation());
	}

	private SecurityEventRequest validRequest(
			SecurityRuleRequest rule,
			GeoLocationRequest geoLocation) {
		return new SecurityEventRequest(
				"evt-00132",
				Instant.parse("2026-05-20T14:32:10Z"),
				14227L,
				"pol_web1",
				"203.0.113.42",
				"www.example.com",
				"/api/v1/login",
				"POST",
				403,
				"Mozilla/5.0",
				rule,
				EventAction.DENY,
				geoLocation,
				1024L,
				256L);
	}

	private SecurityRuleRequest validRule() {
		return new SecurityRuleRequest(
				"950001",
				"SQL_INJECTION",
				"SQL Injection Attack Detected",
				Severity.CRITICAL,
				RuleCategory.INJECTION);
	}

	private GeoLocationRequest validGeoLocation() {
		return new GeoLocationRequest("CN", "Beijing");
	}
}

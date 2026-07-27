package com.rachelikatz.miniwsa.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ConfigurationPropertiesValidationTests {

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MiniWsaConfiguration.class);

	@Test
	void rejectsNonPositiveMaximumBatchSize() {
		IngestionProperties properties = new IngestionProperties();
		properties.setMaxBatchSize(0);

		assertThat(VALIDATOR.validate(properties))
				.extracting(violation -> violation.getPropertyPath().toString())
				.containsExactly("maxBatchSize");
	}

	@Test
	void rejectsNonPositiveRateLimit() {
		RateLimitProperties properties = new RateLimitProperties();
		properties.setRequestsPerMinute(-1);

		assertThat(VALIDATOR.validate(properties))
				.extracting(violation -> violation.getPropertyPath().toString())
				.containsExactly("requestsPerMinute");
	}

	@Test
	void failsStartupForInvalidMaximumBatchSize() {
		contextRunner
				.withPropertyValues("miniwsa.ingest.max-batch-size=0")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).hasMessageContaining("miniwsa.ingest");
				});
	}

	@Test
	void failsStartupForInvalidRateLimit() {
		contextRunner
				.withPropertyValues("miniwsa.ratelimit.requests-per-minute=0")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).hasMessageContaining("miniwsa.ratelimit");
				});
	}
}

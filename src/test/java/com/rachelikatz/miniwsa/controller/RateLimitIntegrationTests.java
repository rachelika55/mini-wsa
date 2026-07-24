package com.rachelikatz.miniwsa.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.rachelikatz.miniwsa.persistence.repository.SecurityEventRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"miniwsa.ratelimit.enabled=true",
		"miniwsa.ratelimit.requests-per-minute=3"
})
@AutoConfigureRestTestClient
class RateLimitIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Autowired
	private SecurityEventRepository repository;

	@BeforeEach
	void clearData() {
		repository.deleteAll();
	}

	@Test
	void throttlesReadApisPerClientBeyondTheLimit() {
		// The limit is shared across the read APIs (stats + samples) per client IP.
		restTestClient.get().uri("/v1/events/samples").exchange().expectStatus().isOk();
		restTestClient.get().uri("/v1/events/samples").exchange().expectStatus().isOk();
		restTestClient.get()
				.uri("/v1/stats/summary?from=2026-05-20T00:00:00Z&to=2026-05-21T00:00:00Z")
				.exchange().expectStatus().isOk();

		// Fourth request within the window is rejected.
		restTestClient.get()
				.uri("/v1/events/samples")
				.exchange()
				.expectStatus().isEqualTo(429)
				.expectBody()
				.jsonPath("$.status").isEqualTo(429);
	}
}

package com.rachelikatz.miniwsa.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.rachelikatz.miniwsa.persistence.repository.SecurityEventRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"miniwsa.ingest.max-batch-size=2"
})
@AutoConfigureRestTestClient
class IngestionGuardrailsIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Autowired
	private SecurityEventRepository repository;

	@BeforeEach
	void clearData() {
		repository.deleteAll();
	}

	@Test
	void rejectsBatchLargerThanTheConfiguredMaximum() {
		String batch = "[" + String.join(",", event("e1"), event("e2"), event("e3")) + "]";
		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(batch)
				.exchange()
				.expectStatus().isEqualTo(413)
				.expectBody()
				.jsonPath("$.status").isEqualTo(413);
	}

	@Test
	void acceptsBatchAtTheConfiguredMaximum() {
		String batch = "[" + String.join(",", event("e1"), event("e2")) + "]";
		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(batch)
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.ingestedCount").isEqualTo(2);
	}

	private String event(String eventId) {
		return """
				{
				  "eventId": "%s",
				  "timestamp": "2026-05-20T14:00:00Z",
				  "configId": 14227,
				  "policyId": "pol_web1",
				  "clientIp": "203.0.113.42",
				  "hostname": "www.example.com",
				  "path": "/api/v1/login",
				  "method": "POST",
				  "statusCode": 403,
				  "userAgent": "Mozilla/5.0",
				  "rule": { "id": "950001", "name": "RULE", "message": "msg", "severity": "HIGH", "category": "INJECTION" },
				  "action": "DENY",
				  "geoLocation": { "country": "CN", "city": "Beijing" },
				  "requestSize": 1024,
				  "responseSize": 256
				}""".formatted(eventId);
	}
}

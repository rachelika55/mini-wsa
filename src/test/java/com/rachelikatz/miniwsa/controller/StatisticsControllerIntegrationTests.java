package com.rachelikatz.miniwsa.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.rachelikatz.miniwsa.persistence.repository.SecurityEventRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class StatisticsControllerIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Autowired
	private SecurityEventRepository repository;

	@BeforeEach
	void seedData() {
		repository.deleteAll();
		ingest("[" + String.join(",",
				// config 14227
				event("a1", 14227, "203.0.113.42", "2026-05-20T14:00:00Z",
						"/api/v1/login", "INJECTION", "CRITICAL", "DENY"),
				event("a2", 14227, "203.0.113.42", "2026-05-20T14:00:01Z",
						"/api/v1/login", "INJECTION", "HIGH", "ALERT"),
				event("a3", 14227, "203.0.113.43", "2026-05-20T14:00:02Z",
						"/robots.txt", "BOT", "LOW", "MONITOR"),
				// config 99999
				event("b1", 99999, "198.51.100.50", "2026-05-20T14:00:03Z",
						"/search", "XSS", "MEDIUM", "DENY"))
				+ "]");
	}

	@Test
	void summarizesSingleConfigWithDatabaseAggregation() {
		restTestClient.get()
				.uri("/v1/stats/summary?configId=14227&from=2026-05-20T13:00:00Z&to=2026-05-20T15:00:00Z")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.configId").isEqualTo(14227)
				.jsonPath("$.totalEvents").isEqualTo(3)
				.jsonPath("$.byCategory.INJECTION.count").isEqualTo(2)
				.jsonPath("$.byCategory.INJECTION.avgThreatScore").isEqualTo(65.0)
				.jsonPath("$.byCategory.BOT.count").isEqualTo(1)
				.jsonPath("$.byCategory.BOT.avgThreatScore").isEqualTo(10.0)
				.jsonPath("$.byAction.DENY").isEqualTo(1)
				.jsonPath("$.byAction.ALERT").isEqualTo(1)
				.jsonPath("$.byAction.MONITOR").isEqualTo(1)
				.jsonPath("$.topAttackers[0].clientIp").isEqualTo("203.0.113.42")
				.jsonPath("$.topAttackers[0].count").isEqualTo(2)
				.jsonPath("$.topAttackers[0].avgThreatScore").isEqualTo(65.0)
				.jsonPath("$.topTargetedPaths[0].path").isEqualTo("/api/v1/login")
				.jsonPath("$.topTargetedPaths[0].count").isEqualTo(2);
	}

	@Test
	void aggregatesAcrossAllConfigsWhenConfigIdOmitted() {
		restTestClient.get()
				.uri("/v1/stats/summary?from=2026-05-20T13:00:00Z&to=2026-05-20T15:00:00Z")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.configId").doesNotExist()
				.jsonPath("$.totalEvents").isEqualTo(4);
	}

	@Test
	void excludesEventsOutsideTimeRange() {
		restTestClient.get()
				.uri("/v1/stats/summary?from=2026-05-20T14:00:02Z&to=2026-05-20T15:00:00Z")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalEvents").isEqualTo(2);
	}

	@Test
	void usesInclusiveFromAndExclusiveToBoundaries() {
		restTestClient.get()
				.uri("/v1/stats/summary?from=2026-05-20T14:00:01Z&to=2026-05-20T14:00:03Z")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalEvents").isEqualTo(2)
				.jsonPath("$.topAttackers[0].clientIp").isEqualTo("203.0.113.42");
	}

	@Test
	void returnsEmptyAggregatesWhenNoEventsMatch() {
		restTestClient.get()
				.uri("/v1/stats/summary?configId=123456&from=2026-05-20T13:00:00Z&to=2026-05-20T15:00:00Z")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalEvents").isEqualTo(0)
				.jsonPath("$.byCategory").isEmpty()
				.jsonPath("$.byAction").isEmpty()
				.jsonPath("$.topAttackers").isEmpty()
				.jsonPath("$.topTargetedPaths").isEmpty();
	}

	@Test
	void ordersEqualCountsDeterministically() {
		restTestClient.get()
				.uri("/v1/stats/summary?from=2026-05-20T13:00:00Z&to=2026-05-20T15:00:00Z")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.topAttackers[0].clientIp").isEqualTo("203.0.113.42")
				.jsonPath("$.topAttackers[1].clientIp").isEqualTo("198.51.100.50")
				.jsonPath("$.topTargetedPaths[0].path").isEqualTo("/api/v1/login")
				.jsonPath("$.topTargetedPaths[1].path").isEqualTo("/robots.txt");
	}

	@Test
	void limitsTopListsToTenEntries() {
		StringBuilder batch = new StringBuilder("[");
		for (int i = 1; i <= 12; i++) {
			if (i > 1) {
				batch.append(',');
			}
			batch.append(event(
					"top-" + i,
					777,
					"192.0.2." + i,
					"2026-05-20T14:10:00Z",
					"/target-" + i,
					"BOT",
					"LOW",
					"MONITOR"));
		}
		batch.append(']');
		ingest(batch.toString());

		restTestClient.get()
				.uri("/v1/stats/summary?configId=777&from=2026-05-20T13:00:00Z&to=2026-05-20T15:00:00Z")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalEvents").isEqualTo(12)
				.jsonPath("$.topAttackers.length()").isEqualTo(10)
				.jsonPath("$.topTargetedPaths.length()").isEqualTo(10)
				.jsonPath("$.topAttackers[0].clientIp").isEqualTo("192.0.2.1")
				.jsonPath("$.topTargetedPaths[0].path").isEqualTo("/target-1");
	}

	@Test
	void rejectsInvertedTimeRange() {
		restTestClient.get()
				.uri("/v1/stats/summary?from=2026-05-20T15:00:00Z&to=2026-05-20T14:00:00Z")
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void rejectsEqualTimeRange() {
		restTestClient.get()
				.uri("/v1/stats/summary?from=2026-05-20T14:00:00Z&to=2026-05-20T14:00:00Z")
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void rejectsMalformedTimestamp() {
		restTestClient.get()
				.uri("/v1/stats/summary?from=not-a-timestamp&to=2026-05-20T15:00:00Z")
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void rejectsMalformedConfigIdAsBadRequest() {
		restTestClient.get()
				.uri("/v1/stats/summary?configId=not-a-number&from=2026-05-20T13:00:00Z&to=2026-05-20T15:00:00Z")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.status").isEqualTo(400)
				.jsonPath("$.message").isEqualTo("Invalid value for query parameter 'configId'");
	}

	@Test
	void requiresFromTimestamp() {
		restTestClient.get()
				.uri("/v1/stats/summary?to=2026-05-20T15:00:00Z")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.message").isEqualTo("'from' is required");
	}

	@Test
	void requiresToTimestamp() {
		restTestClient.get()
				.uri("/v1/stats/summary?from=2026-05-20T13:00:00Z")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.message").isEqualTo("'to' is required");
	}

	private void ingest(String body) {
		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.exchange()
				.expectStatus().isCreated();
	}

	private String event(
			String eventId,
			long configId,
			String clientIp,
			String timestamp,
			String path,
			String category,
			String severity,
			String action) {
		return """
				{
				  "eventId": "%s",
				  "timestamp": "%s",
				  "configId": %d,
				  "policyId": "pol_web1",
				  "clientIp": "%s",
				  "hostname": "www.example.com",
				  "path": "%s",
				  "method": "POST",
				  "statusCode": 403,
				  "userAgent": "Mozilla/5.0",
				  "rule": {
				    "id": "950001",
				    "name": "RULE",
				    "message": "Attack Detected",
				    "severity": "%s",
				    "category": "%s"
				  },
				  "action": "%s",
				  "geoLocation": { "country": "CN", "city": "Beijing" },
				  "requestSize": 1024,
				  "responseSize": 256
				}""".formatted(eventId, timestamp, configId, clientIp, path, severity, category, action);
	}
}

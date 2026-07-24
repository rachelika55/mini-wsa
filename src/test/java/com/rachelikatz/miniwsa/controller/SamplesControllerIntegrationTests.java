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
class SamplesControllerIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Autowired
	private SecurityEventRepository repository;

	@BeforeEach
	void seedData() {
		repository.deleteAll();
		ingest("[" + String.join(",",
				event("s1", 14227, "203.0.113.42", "2026-05-20T14:00:00Z",
						"/api/v1/login", "INJECTION", "CRITICAL", "DENY"),
				event("s2", 14227, "203.0.113.42", "2026-05-20T14:00:01Z",
						"/api/v1/login", "XSS", "HIGH", "ALERT"),
				event("s3", 14227, "203.0.113.43", "2026-05-20T14:00:02Z",
						"/robots.txt", "BOT", "LOW", "MONITOR"),
				event("s4", 99999, "198.51.100.50", "2026-05-20T14:00:03Z",
						"/search", "XSS", "MEDIUM", "DENY"),
				event("s5", 99999, "198.51.100.50", "2026-05-20T14:00:04Z",
						"/search", "DOS", "MEDIUM", "ALERT"))
				+ "]");
	}

	@Test
	void returnsAllEventsNewestFirstWithDefaults() {
		restTestClient.get()
				.uri("/v1/events/samples")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalCount").isEqualTo(5)
				.jsonPath("$.limit").isEqualTo(20)
				.jsonPath("$.offset").isEqualTo(0)
				.jsonPath("$.items.length()").isEqualTo(5)
				.jsonPath("$.items[0].eventId").isEqualTo("s5")
				.jsonPath("$.items[4].eventId").isEqualTo("s1");
	}

	@Test
	void returnsNestedShapeWithHumanReadableAttackType() {
		restTestClient.get()
				.uri("/v1/events/samples?limit=1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.items[0].eventId").isEqualTo("s5")
				.jsonPath("$.items[0].attackType").isEqualTo("Denial of Service")
				.jsonPath("$.items[0].threatScore").isNumber()
				.jsonPath("$.items[0].receivedAt").exists()
				.jsonPath("$.items[0].rule.category").isEqualTo("DOS")
				.jsonPath("$.items[0].rule.severity").isEqualTo("MEDIUM")
				.jsonPath("$.items[0].action").isEqualTo("ALERT")
				.jsonPath("$.items[0].geoLocation.country").isEqualTo("CN")
				.jsonPath("$.items[0].geoLocation.city").isEqualTo("Beijing");
	}

	@Test
	void filtersByConfigId() {
		restTestClient.get()
				.uri("/v1/events/samples?configId=99999")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalCount").isEqualTo(2)
				.jsonPath("$.items.length()").isEqualTo(2)
				.jsonPath("$.items[0].eventId").isEqualTo("s5")
				.jsonPath("$.items[1].eventId").isEqualTo("s4");
	}

	@Test
	void filtersByCategory() {
		restTestClient.get()
				.uri("/v1/events/samples?category=XSS")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalCount").isEqualTo(2)
				.jsonPath("$.items[0].eventId").isEqualTo("s4")
				.jsonPath("$.items[1].eventId").isEqualTo("s2");
	}

	@Test
	void filtersByAction() {
		restTestClient.get()
				.uri("/v1/events/samples?action=DENY")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalCount").isEqualTo(2)
				.jsonPath("$.items[0].eventId").isEqualTo("s4")
				.jsonPath("$.items[1].eventId").isEqualTo("s1");
	}

	@Test
	void appliesInclusiveFromAndExclusiveToBoundaries() {
		restTestClient.get()
				.uri("/v1/events/samples?from=2026-05-20T14:00:01Z&to=2026-05-20T14:00:03Z")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalCount").isEqualTo(2)
				.jsonPath("$.items[0].eventId").isEqualTo("s3")
				.jsonPath("$.items[1].eventId").isEqualTo("s2");
	}

	@Test
	void appliesLimit() {
		restTestClient.get()
				.uri("/v1/events/samples?limit=2")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalCount").isEqualTo(5)
				.jsonPath("$.limit").isEqualTo(2)
				.jsonPath("$.items.length()").isEqualTo(2)
				.jsonPath("$.items[0].eventId").isEqualTo("s5")
				.jsonPath("$.items[1].eventId").isEqualTo("s4");
	}

	@Test
	void appliesOffsetForPaging() {
		restTestClient.get()
				.uri("/v1/events/samples?limit=2&offset=2")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalCount").isEqualTo(5)
				.jsonPath("$.limit").isEqualTo(2)
				.jsonPath("$.offset").isEqualTo(2)
				.jsonPath("$.items.length()").isEqualTo(2)
				.jsonPath("$.items[0].eventId").isEqualTo("s3")
				.jsonPath("$.items[1].eventId").isEqualTo("s2");
	}

	@Test
	void returnsEmptyItemsWhenOffsetBeyondEndButKeepsTotalCount() {
		restTestClient.get()
				.uri("/v1/events/samples?offset=10")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalCount").isEqualTo(5)
				.jsonPath("$.items.length()").isEqualTo(0);
	}

	@Test
	void rejectsLimitAboveMax() {
		restTestClient.get()
				.uri("/v1/events/samples?limit=101")
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void rejectsLimitBelowOne() {
		restTestClient.get()
				.uri("/v1/events/samples?limit=0")
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void rejectsNegativeOffset() {
		restTestClient.get()
				.uri("/v1/events/samples?offset=-1")
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void rejectsInvertedTimeRange() {
		restTestClient.get()
				.uri("/v1/events/samples?from=2026-05-20T15:00:00Z&to=2026-05-20T14:00:00Z")
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void rejectsMalformedTimestamp() {
		restTestClient.get()
				.uri("/v1/events/samples?from=not-a-timestamp")
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void rejectsInvalidCategoryAsBadRequest() {
		restTestClient.get()
				.uri("/v1/events/samples?category=NOT_A_CATEGORY")
				.exchange()
				.expectStatus().isBadRequest();
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

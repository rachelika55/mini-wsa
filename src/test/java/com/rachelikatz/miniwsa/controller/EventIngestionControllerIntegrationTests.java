package com.rachelikatz.miniwsa.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.rachelikatz.miniwsa.domain.AttackType;
import com.rachelikatz.miniwsa.persistence.entity.SecurityEventEntity;
import com.rachelikatz.miniwsa.persistence.repository.SecurityEventRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class EventIngestionControllerIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Autowired
	private SecurityEventRepository repository;

	@BeforeEach
	void clearData() {
		repository.deleteAll();
	}

	@Test
	void ingestsSingleEventAndStoresEnrichedFields() {
		String body = event("evt-1", "203.0.113.42", "2026-05-20T14:32:10Z",
				"/api/v1/login", "CRITICAL", "DENY");

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.ingestedCount").isEqualTo(1)
				.jsonPath("$.eventIds[0]").isEqualTo("evt-1");

		SecurityEventEntity stored = repository.findById("evt-1").orElseThrow();
		assertThat(stored.getAttackType()).isEqualTo(AttackType.SQL_COMMAND_INJECTION);
		assertThat(stored.getThreatScore()).isEqualTo(75);
		assertThat(stored.getReceivedAt()).isNotNull();
	}

	@Test
	void ingestsBatchOfEvents() {
		String body = "[" + event("evt-1", "203.0.113.42", "2026-05-20T14:32:10Z",
				"/api/orders", "LOW", "MONITOR")
				+ "," + event("evt-2", "203.0.113.42", "2026-05-20T14:33:10Z",
				"/api/orders", "LOW", "MONITOR") + "]";

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.ingestedCount").isEqualTo(2);

		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	void rejectsEventMissingRequiredFields() {
		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body("{}")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.status").isEqualTo(400)
				.jsonPath("$.violations").isNotEmpty();

		assertThat(repository.count()).isZero();
	}

	@Test
	void rejectsMalformedJson() {
		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body("{ not json")
				.exchange()
				.expectStatus().isBadRequest();
	}

	@Test
	void rejectsEmptyBatch() {
		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body("[]")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.message").isEqualTo("Validation failed for one or more events");
	}

	@Test
	void rejectsValuesOutsideApiAndStorageBounds() {
		String body = event("evt-invalid", "203.0.113.42", "2026-05-20T14:32:10Z",
				"/api/orders", "LOW", "MONITOR")
				.replace("\"hostname\": \"www.example.com\"", "\"hostname\": \"" + "a".repeat(256) + "\"")
				.replace("\"statusCode\": 403", "\"statusCode\": 99")
				.replace("\"requestSize\": 1024", "\"requestSize\": -1");

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.violations.length()").isEqualTo(3);

		assertThat(repository.count()).isZero();
	}

	@Test
	void rejectsDuplicateEventIdWithinBatch() {
		String body = "[" + event("evt-dup", "203.0.113.42", "2026-05-20T14:32:10Z",
				"/api/orders", "LOW", "MONITOR")
				+ "," + event("evt-dup", "203.0.113.42", "2026-05-20T14:33:10Z",
				"/api/orders", "LOW", "MONITOR") + "]";

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.exchange()
				.expectStatus().isEqualTo(409);

		assertThat(repository.count()).isZero();
	}

	@Test
	void rejectsEventIdThatAlreadyExists() {
		String body = event("evt-existing", "203.0.113.42", "2026-05-20T14:32:10Z",
				"/api/orders", "LOW", "MONITOR");

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.exchange()
				.expectStatus().isCreated();

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.exchange()
				.expectStatus().isEqualTo(409)
				.expectBody()
				.jsonPath("$.message").isEqualTo("An event with the same ID already exists");

		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void appliesRepeatOffenderBonusToTheSixthEvent() {
		StringBuilder batch = new StringBuilder("[");
		for (int i = 0; i < 6; i++) {
			if (i > 0) {
				batch.append(',');
			}
			String timestamp = "2026-05-20T14:00:0" + i + "Z";
			batch.append(event("evt-" + i, "203.0.113.99", timestamp,
					"/api/orders", "LOW", "MONITOR"));
		}
		batch.append(']');

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(batch.toString())
				.exchange()
				.expectStatus().isCreated();

		assertThat(repository.findById("evt-4").orElseThrow().getThreatScore()).isEqualTo(10);
		assertThat(repository.findById("evt-5").orElseThrow().getThreatScore()).isEqualTo(25);
	}

	@Test
	void countsEarlierEventsAtTheSameTimestamp() {
		StringBuilder batch = new StringBuilder("[");
		for (int i = 0; i < 6; i++) {
			if (i > 0) {
				batch.append(',');
			}
			batch.append(event(
					"evt-" + i,
					"203.0.113.99",
					"2026-05-20T14:00:00Z",
					"/api/orders",
					"LOW",
					"MONITOR"));
		}
		batch.append(']');

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(batch.toString())
				.exchange()
				.expectStatus().isCreated();

		assertThat(repository.findById("evt-4").orElseThrow().getThreatScore()).isEqualTo(10);
		assertThat(repository.findById("evt-5").orElseThrow().getThreatScore()).isEqualTo(25);
	}

	@Test
	void includesAnEventExactlyTenMinutesBeforeTheCurrentEvent() {
		String[] timestamps = {
				"2026-05-20T13:50:00Z",
				"2026-05-20T13:51:00Z",
				"2026-05-20T13:52:00Z",
				"2026-05-20T13:53:00Z",
				"2026-05-20T13:59:00Z",
				"2026-05-20T14:00:00Z"
		};
		StringBuilder batch = new StringBuilder("[");
		for (int i = 0; i < timestamps.length; i++) {
			if (i > 0) {
				batch.append(',');
			}
			batch.append(event(
					"evt-" + i,
					"203.0.113.99",
					timestamps[i],
					"/api/orders",
					"LOW",
					"MONITOR"));
		}
		batch.append(']');

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(batch.toString())
				.exchange()
				.expectStatus().isCreated();

		assertThat(repository.findById("evt-5").orElseThrow().getThreatScore()).isEqualTo(25);
	}

	@Test
	void scoresOutOfOrderBatchByEventTime() {
		int[] requestOrder = {5, 0, 4, 1, 3, 2};
		StringBuilder batch = new StringBuilder("[");
		for (int index = 0; index < requestOrder.length; index++) {
			if (index > 0) {
				batch.append(',');
			}
			int eventNumber = requestOrder[index];
			batch.append(event(
					"evt-" + eventNumber,
					"203.0.113.99",
					"2026-05-20T14:00:0" + eventNumber + "Z",
					"/api/orders",
					"LOW",
					"MONITOR"));
		}
		batch.append(']');

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(batch.toString())
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.eventIds[0]").isEqualTo("evt-5");

		assertThat(repository.findById("evt-4").orElseThrow().getThreatScore()).isEqualTo(10);
		assertThat(repository.findById("evt-5").orElseThrow().getThreatScore()).isEqualTo(25);
	}

	@Test
	void countsPersistedEventsWhenScoringTheSixthEvent() {
		StringBuilder firstFive = new StringBuilder("[");
		for (int i = 0; i < 5; i++) {
			if (i > 0) {
				firstFive.append(',');
			}
			firstFive.append(event(
					"evt-" + i,
					"203.0.113.99",
					"2026-05-20T14:00:0" + i + "Z",
					"/api/orders",
					"LOW",
					"MONITOR"));
		}
		firstFive.append(']');

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(firstFive.toString())
				.exchange()
				.expectStatus().isCreated();

		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.APPLICATION_JSON)
				.body(event(
						"evt-5",
						"203.0.113.99",
						"2026-05-20T14:00:05Z",
						"/api/orders",
						"LOW",
						"MONITOR"))
				.exchange()
				.expectStatus().isCreated();

		assertThat(repository.findById("evt-5").orElseThrow().getThreatScore()).isEqualTo(25);
	}

	private String event(
			String eventId,
			String clientIp,
			String timestamp,
			String path,
			String severity,
			String action) {
		return """
				{
				  "eventId": "%s",
				  "timestamp": "%s",
				  "configId": 14227,
				  "policyId": "pol_web1",
				  "clientIp": "%s",
				  "hostname": "www.example.com",
				  "path": "%s",
				  "method": "POST",
				  "statusCode": 403,
				  "userAgent": "Mozilla/5.0",
				  "rule": {
				    "id": "950001",
				    "name": "SQL_INJECTION",
				    "message": "SQL Injection Attack Detected",
				    "severity": "%s",
				    "category": "INJECTION"
				  },
				  "action": "%s",
				  "geoLocation": { "country": "CN", "city": "Beijing" },
				  "requestSize": 1024,
				  "responseSize": 256
				}""".formatted(eventId, timestamp, clientIp, path, severity, action);
	}
}

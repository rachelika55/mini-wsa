package com.rachelikatz.miniwsa.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class HttpErrorIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void returnsNotFoundForUnknownRoute() {
		restTestClient.get()
				.uri("/v1/not-a-route")
				.exchange()
				.expectStatus().isNotFound()
				.expectBody()
				.jsonPath("$.status").isEqualTo(404)
				.jsonPath("$.message").isEqualTo("Resource not found");
	}

	@Test
	void returnsMethodNotAllowedForUnsupportedMethod() {
		restTestClient.get()
				.uri("/v1/events/ingest")
				.exchange()
				.expectStatus().isEqualTo(405)
				.expectHeader().valueEquals(HttpHeaders.ALLOW, "POST")
				.expectBody()
				.jsonPath("$.status").isEqualTo(405)
				.jsonPath("$.message").isEqualTo("Request method is not supported");
	}

	@Test
	void returnsUnsupportedMediaTypeForNonJsonIngestion() {
		restTestClient.post()
				.uri("/v1/events/ingest")
				.contentType(MediaType.TEXT_PLAIN)
				.body("{}")
				.exchange()
				.expectStatus().isEqualTo(415)
				.expectBody()
				.jsonPath("$.status").isEqualTo(415)
				.jsonPath("$.message").isEqualTo("Content type is not supported");
	}
}

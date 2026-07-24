package com.rachelikatz.miniwsa.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class HealthControllerIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void returnsUpStatus() {
		restTestClient.get()
				.uri("/health")
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("{\"status\":\"UP\"}");
	}
}

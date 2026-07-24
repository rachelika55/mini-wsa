package com.rachelikatz.miniwsa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rachelikatz.miniwsa.dto.IngestionResponse;
import com.rachelikatz.miniwsa.dto.SecurityEventRequest;
import com.rachelikatz.miniwsa.service.EventIngestionService;
import com.rachelikatz.miniwsa.service.IngestionPayloadReader;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/v1/events")
public class EventIngestionController {

	private final IngestionPayloadReader payloadReader;
	private final EventIngestionService ingestionService;

	public EventIngestionController(
			IngestionPayloadReader payloadReader,
			EventIngestionService ingestionService) {
		this.payloadReader = payloadReader;
		this.ingestionService = ingestionService;
	}

	@PostMapping(path = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IngestionResponse> ingest(@RequestBody JsonNode payload) {
		List<SecurityEventRequest> events = payloadReader.read(payload);
		IngestionResponse response = ingestionService.ingest(events);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}

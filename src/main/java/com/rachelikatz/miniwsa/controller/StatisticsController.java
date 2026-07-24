package com.rachelikatz.miniwsa.controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rachelikatz.miniwsa.dto.SummaryResponse;
import com.rachelikatz.miniwsa.exception.InvalidRequestException;
import com.rachelikatz.miniwsa.service.StatisticsService;

@RestController
@RequestMapping("/v1/stats")
public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping("/summary")
	public SummaryResponse summary(
			@RequestParam(required = false) Long configId,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to) {
		Instant fromInstant = parseRequiredInstant(from, "from");
		Instant toInstant = parseRequiredInstant(to, "to");
		return statisticsService.summarize(configId, fromInstant, toInstant);
	}

	private Instant parseRequiredInstant(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new InvalidRequestException("'" + field + "' is required");
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException ex) {
			throw new InvalidRequestException(
					"'" + field + "' must be an ISO-8601 instant, e.g. 2026-05-20T14:32:10Z");
		}
	}
}

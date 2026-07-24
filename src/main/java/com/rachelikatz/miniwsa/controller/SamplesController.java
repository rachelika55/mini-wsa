package com.rachelikatz.miniwsa.controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.dto.SamplesResponse;
import com.rachelikatz.miniwsa.exception.InvalidRequestException;
import com.rachelikatz.miniwsa.service.SamplesService;

@RestController
@RequestMapping("/v1/events")
public class SamplesController {

	private final SamplesService samplesService;

	public SamplesController(SamplesService samplesService) {
		this.samplesService = samplesService;
	}

	@GetMapping("/samples")
	public SamplesResponse samples(
			@RequestParam(required = false) Long configId,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to,
			@RequestParam(required = false) RuleCategory category,
			@RequestParam(required = false) EventAction action,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) Integer offset) {
		Instant fromInstant = parseOptionalInstant(from, "from");
		Instant toInstant = parseOptionalInstant(to, "to");
		return samplesService.getSamples(configId, fromInstant, toInstant, category, action, limit, offset);
	}

	private Instant parseOptionalInstant(String value, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException ex) {
			throw new InvalidRequestException(
					"'" + field + "' must be an ISO-8601 instant, e.g. 2026-05-20T14:32:10Z");
		}
	}
}

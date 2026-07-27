package com.rachelikatz.miniwsa.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.rachelikatz.miniwsa.config.IngestionProperties;
import com.rachelikatz.miniwsa.dto.SecurityEventRequest;
import com.rachelikatz.miniwsa.exception.FieldViolation;
import com.rachelikatz.miniwsa.exception.IngestionValidationException;
import com.rachelikatz.miniwsa.exception.PayloadTooLargeException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Normalizes the ingestion body, which may be a single event object or a
 * non-empty array of events, into a validated list. Deserialization and Bean
 * Validation failures are collected with the offending item position so the
 * whole request can be rejected atomically with actionable details.
 */
@Component
public class IngestionPayloadReader {

	private final ObjectMapper objectMapper;
	private final Validator validator;
	private final IngestionProperties properties;

	public IngestionPayloadReader(
			ObjectMapper objectMapper,
			Validator validator,
			IngestionProperties properties) {
		this.objectMapper = objectMapper;
		this.validator = validator;
		this.properties = properties;
	}

	public List<SecurityEventRequest> read(JsonNode payload) {
		if (payload == null || payload.isNull() || payload.isMissingNode()) {
			throw new IngestionValidationException(
					List.of(new FieldViolation("payload", "must not be empty")));
		}

		boolean batch = payload.isArray();
		List<JsonNode> nodes = new ArrayList<>();
		if (batch) {
			if (payload.isEmpty()) {
				throw new IngestionValidationException(
						List.of(new FieldViolation("payload", "batch must not be empty")));
			}
			if (payload.size() > properties.getMaxBatchSize()) {
				throw new PayloadTooLargeException(
						"Batch size " + payload.size() + " exceeds the maximum of "
								+ properties.getMaxBatchSize());
			}
			payload.forEach(nodes::add);
		} else if (payload.isObject()) {
			nodes.add(payload);
		} else {
			throw new IngestionValidationException(
					List.of(new FieldViolation("payload", "must be a JSON object or array of objects")));
		}

		List<FieldViolation> violations = new ArrayList<>();
		List<SecurityEventRequest> events = new ArrayList<>(nodes.size());

		for (int i = 0; i < nodes.size(); i++) {
			JsonNode node = nodes.get(i);
			String prefix = batch ? "[" + i + "]" : "";

			if (!node.isObject()) {
				violations.add(new FieldViolation(fieldName(prefix, ""), "must be a JSON object"));
				continue;
			}
			JsonNode timestamp = node.get("timestamp");
			if (timestamp != null && !timestamp.isNull() && !timestamp.isString()) {
				violations.add(new FieldViolation(
						fieldName(prefix, "timestamp"),
						"must be an ISO-8601 string"));
				continue;
			}

			SecurityEventRequest event;
			try {
				event = objectMapper.treeToValue(node, SecurityEventRequest.class);
			} catch (JacksonException ex) {
				String message = ex instanceof UnrecognizedPropertyException ? "unknown field" : "invalid value";
				violations.add(new FieldViolation(fieldName(prefix, invalidPath(ex)), message));
				continue;
			} catch (RuntimeException ex) {
				violations.add(new FieldViolation(fieldName(prefix, ""), "malformed event JSON"));
				continue;
			}

			for (ConstraintViolation<SecurityEventRequest> violation : validator.validate(event)) {
				violations.add(new FieldViolation(
						fieldName(prefix, violation.getPropertyPath().toString()),
						violation.getMessage()));
			}
			events.add(event);
		}

		if (!violations.isEmpty()) {
			throw new IngestionValidationException(violations);
		}
		return events;
	}

	private String invalidPath(JacksonException ex) {
		StringBuilder path = new StringBuilder();
		for (var reference : ex.getPath()) {
			if (reference.getPropertyName() == null) {
				continue;
			}
			if (path.length() > 0) {
				path.append('.');
			}
			path.append(reference.getPropertyName());
		}
		return path.toString();
	}

	private String fieldName(String prefix, String field) {
		if (prefix.isEmpty()) {
			return field.isEmpty() ? "payload" : field;
		}
		return field.isEmpty() ? prefix : prefix + "." + field;
	}
}

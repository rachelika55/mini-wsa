package com.rachelikatz.miniwsa.exception;

import java.time.Clock;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private final Clock clock;

	public GlobalExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(IngestionValidationException.class)
	public ResponseEntity<ApiError> handleValidation(
			IngestionValidationException ex, WebRequest request) {
		ApiError body = new ApiError(
				clock.instant(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				path(request),
				ex.getViolations());
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<ApiError> handleInvalidRequest(
			InvalidRequestException ex, WebRequest request) {
		ApiError body = ApiError.of(
				clock.instant(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				path(request));
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(
			MethodArgumentTypeMismatchException ex, WebRequest request) {
		ApiError body = ApiError.of(
				clock.instant(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Invalid value for query parameter '" + ex.getName() + "'",
				path(request));
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(
			HttpMessageNotReadableException ex, WebRequest request) {
		ApiError body = ApiError.of(
				clock.instant(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Malformed JSON request body",
				path(request));
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(DuplicateEventException.class)
	public ResponseEntity<ApiError> handleDuplicate(
			DuplicateEventException ex, WebRequest request) {
		List<FieldViolation> violations = ex.getEventIds().stream()
				.map(id -> new FieldViolation("eventId", "duplicate event ID '" + id + "'"))
				.toList();
		ApiError body = new ApiError(
				clock.instant(),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				ex.getMessage(),
				path(request),
				violations);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataIntegrity(
			DataIntegrityViolationException ex, WebRequest request) {
		ApiError body = ApiError.of(
				clock.instant(),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				"An event with the same ID already exists",
				path(request));
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
		ApiError body = ApiError.of(
				clock.instant(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				"Unexpected error",
				path(request));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

	private String path(WebRequest request) {
		if (request instanceof ServletWebRequest servletRequest) {
			return servletRequest.getRequest().getRequestURI();
		}
		return null;
	}
}

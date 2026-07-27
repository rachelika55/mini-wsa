package com.rachelikatz.miniwsa.exception;

import java.sql.SQLException;
import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

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

	@ExceptionHandler(PayloadTooLargeException.class)
	public ResponseEntity<ApiError> handlePayloadTooLarge(
			PayloadTooLargeException ex, WebRequest request) {
		ApiError body = ApiError.of(
				clock.instant(),
				HttpStatus.CONTENT_TOO_LARGE.value(),
				HttpStatus.CONTENT_TOO_LARGE.getReasonPhrase(),
				ex.getMessage(),
				path(request));
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(body);
	}

	@ExceptionHandler(RateLimitExceededException.class)
	public ResponseEntity<ApiError> handleRateLimit(
			RateLimitExceededException ex, WebRequest request) {
		ApiError body = ApiError.of(
				clock.instant(),
				HttpStatus.TOO_MANY_REQUESTS.value(),
				HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
				ex.getMessage(),
				path(request));
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
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
		if (!hasSqlState(ex, UNIQUE_VIOLATION_SQL_STATE)) {
			return internalServerError(ex, request);
		}
		ApiError body = ApiError.of(
				clock.instant(),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				"An event with the same ID already exists",
				path(request));
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(
			NoResourceFoundException ex, WebRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, "Resource not found", request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> handleMethodNotAllowed(
			HttpRequestMethodNotSupportedException ex, WebRequest request) {
		return errorResponse(
				HttpStatus.METHOD_NOT_ALLOWED,
				"Request method is not supported",
				request,
				ex.getHeaders());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiError> handleUnsupportedMediaType(
			HttpMediaTypeNotSupportedException ex, WebRequest request) {
		return errorResponse(
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"Content type is not supported",
				request,
				ex.getHeaders());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
		return internalServerError(ex, request);
	}

	private ResponseEntity<ApiError> errorResponse(
			HttpStatus status,
			String message,
			WebRequest request) {
		ApiError body = ApiError.of(
				clock.instant(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path(request));
		return ResponseEntity.status(status).body(body);
	}

	private ResponseEntity<ApiError> errorResponse(
			HttpStatus status,
			String message,
			WebRequest request,
			HttpHeaders headers) {
		ApiError body = ApiError.of(
				clock.instant(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path(request));
		return ResponseEntity.status(status).headers(headers).body(body);
	}

	private ResponseEntity<ApiError> internalServerError(Exception ex, WebRequest request) {
		LOGGER.error("Unhandled request failure for {}", path(request), ex);
		ApiError body = ApiError.of(
				clock.instant(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				"Unexpected error",
				path(request));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

	private boolean hasSqlState(Throwable throwable, String expectedState) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SQLException sqlException
					&& expectedState.equals(sqlException.getSQLState())) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private String path(WebRequest request) {
		if (request instanceof ServletWebRequest servletRequest) {
			return servletRequest.getRequest().getRequestURI();
		}
		return null;
	}
}

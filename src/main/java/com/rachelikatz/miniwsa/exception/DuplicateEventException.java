package com.rachelikatz.miniwsa.exception;

import java.util.List;

public class DuplicateEventException extends RuntimeException {

	private final transient List<String> eventIds;

	public DuplicateEventException(String message, List<String> eventIds) {
		super(message);
		this.eventIds = List.copyOf(eventIds);
	}

	public List<String> getEventIds() {
		return eventIds;
	}
}

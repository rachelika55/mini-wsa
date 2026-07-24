package com.rachelikatz.miniwsa.dto;

import java.util.List;

public record IngestionResponse(
		int ingestedCount,
		List<String> eventIds) {
}

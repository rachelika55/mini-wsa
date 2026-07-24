package com.rachelikatz.miniwsa.dto;

import java.util.List;

public record SamplesResponse(
		List<SecurityEventResponse> items,
		long totalCount,
		int limit,
		int offset) {
}

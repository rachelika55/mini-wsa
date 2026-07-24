package com.rachelikatz.miniwsa.persistence.repository;

import java.util.List;

import com.rachelikatz.miniwsa.persistence.entity.SecurityEventEntity;

/**
 * One page of matching events plus the total number of matches across all pages
 * (ignoring limit/offset), so callers can report an accurate {@code totalCount}.
 */
public record SampleQueryResult(
		List<SecurityEventEntity> events,
		long totalCount) {
}

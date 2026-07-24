package com.rachelikatz.miniwsa.persistence.repository;

/**
 * Custom repository fragment for the samples endpoint. Uses the Criteria API so
 * filters can be composed dynamically and an arbitrary {@code offset} can be
 * applied, which the derived-query / {@code Pageable} approach does not express
 * cleanly for standalone offset paging.
 */
public interface SecurityEventSampleQueryRepository {

	SampleQueryResult findSamples(SampleFilter filter);
}

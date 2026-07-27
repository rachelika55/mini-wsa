package com.rachelikatz.miniwsa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * Ingestion tuning knobs. {@code maxBatchSize} rejects oversized batches with
 * 413 so a single request cannot force an unbounded transaction or O(n^2)
 * in-batch repeat-offender scan.
 */
@Validated
@ConfigurationProperties(prefix = "miniwsa.ingest")
public class IngestionProperties {

	@Min(1)
	private int maxBatchSize = 1_000;

	public int getMaxBatchSize() {
		return maxBatchSize;
	}

	public void setMaxBatchSize(int maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}
}

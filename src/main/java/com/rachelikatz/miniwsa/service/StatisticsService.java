package com.rachelikatz.miniwsa.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.rachelikatz.miniwsa.dto.SummaryResponse;
import com.rachelikatz.miniwsa.dto.SummaryResponse.AttackerStats;
import com.rachelikatz.miniwsa.dto.SummaryResponse.CategoryStats;
import com.rachelikatz.miniwsa.dto.SummaryResponse.PathStats;
import com.rachelikatz.miniwsa.dto.SummaryResponse.TimeRange;
import com.rachelikatz.miniwsa.exception.InvalidRequestException;
import com.rachelikatz.miniwsa.persistence.projection.ActionAggregate;
import com.rachelikatz.miniwsa.persistence.projection.AttackerAggregate;
import com.rachelikatz.miniwsa.persistence.projection.CategoryAggregate;
import com.rachelikatz.miniwsa.persistence.repository.SecurityEventRepository;

@Service
public class StatisticsService {

	private static final int TOP_LIMIT = 10;

	private final SecurityEventRepository repository;

	public StatisticsService(SecurityEventRepository repository) {
		this.repository = repository;
	}

	/**
	 * Aggregates statistics entirely in the database over an optional config and
	 * required half-open {@code [from, to)} event-time window. Runs in a read-only,
	 * repeatable-read transaction so every sub-aggregate observes one snapshot.
	 */
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public SummaryResponse summarize(Long configId, Instant from, Instant to) {
		if (from == null || to == null) {
			throw new InvalidRequestException("'from' and 'to' are required");
		}
		if (!from.isBefore(to)) {
			throw new InvalidRequestException("'from' must be strictly before 'to'");
		}

		long totalEvents = repository.countSummary(configId, from, to);

		Map<String, CategoryStats> byCategory = new LinkedHashMap<>();
		for (CategoryAggregate row : repository.aggregateByCategory(configId, from, to)) {
			byCategory.put(
					row.category().name(),
					new CategoryStats(row.count(), round1(row.avgThreatScore())));
		}

		Map<String, Long> byAction = new LinkedHashMap<>();
		for (ActionAggregate row : repository.aggregateByAction(configId, from, to)) {
			byAction.put(row.action().name(), row.count());
		}

		Pageable topTen = PageRequest.of(0, TOP_LIMIT);

		List<AttackerStats> topAttackers = repository.topAttackers(configId, from, to, topTen).stream()
				.map(this::toAttackerStats)
				.toList();

		List<PathStats> topTargetedPaths = repository.topTargetedPaths(configId, from, to, topTen).stream()
				.map(row -> new PathStats(row.path(), row.count()))
				.toList();

		return new SummaryResponse(
				configId,
				new TimeRange(from, to),
				totalEvents,
				byCategory,
				byAction,
				topAttackers,
				topTargetedPaths);
	}

	private AttackerStats toAttackerStats(AttackerAggregate row) {
		return new AttackerStats(row.clientIp(), row.count(), round1(row.avgThreatScore()));
	}

	private double round1(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}
}

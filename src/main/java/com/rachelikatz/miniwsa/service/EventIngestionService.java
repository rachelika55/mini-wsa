package com.rachelikatz.miniwsa.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rachelikatz.miniwsa.domain.AttackType;
import com.rachelikatz.miniwsa.dto.IngestionResponse;
import com.rachelikatz.miniwsa.dto.SecurityEventRequest;
import com.rachelikatz.miniwsa.enrichment.AttackClassifier;
import com.rachelikatz.miniwsa.enrichment.ThreatScoreCalculator;
import com.rachelikatz.miniwsa.exception.DuplicateEventException;
import com.rachelikatz.miniwsa.persistence.entity.SecurityEventEntity;
import com.rachelikatz.miniwsa.persistence.repository.SecurityEventRepository;

/**
 * Orchestrates atomic ingestion of one or more validated security events:
 * server receipt time, attack classification, threat scoring (including the
 * repeat-offender bonus), and persistence all happen in a single transaction.
 */
@Service
public class EventIngestionService {

	private static final Duration REPEAT_WINDOW = Duration.ofMinutes(10);
	private static final long MINIMUM_PRIOR_EVENTS_FOR_REPEAT = 5;

	private final SecurityEventRepository repository;
	private final AttackClassifier attackClassifier;
	private final ThreatScoreCalculator threatScoreCalculator;
	private final Clock clock;

	public EventIngestionService(
			SecurityEventRepository repository,
			AttackClassifier attackClassifier,
			ThreatScoreCalculator threatScoreCalculator,
			Clock clock) {
		this.repository = repository;
		this.attackClassifier = attackClassifier;
		this.threatScoreCalculator = threatScoreCalculator;
		this.clock = clock;
	}

	@Transactional
	public IngestionResponse ingest(List<SecurityEventRequest> events) {
		rejectDuplicateIdsWithinBatch(events);

		Instant receivedAt = clock.instant();
		List<SecurityEventEntity> entities = new ArrayList<>(events.size());
		List<SecurityEventRequest> scoringOrder = events.stream()
				.sorted(Comparator.comparing(SecurityEventRequest::timestamp))
				.toList();

		for (int index = 0; index < scoringOrder.size(); index++) {
			SecurityEventRequest event = scoringOrder.get(index);
			boolean repeatOffender = isRepeatOffender(event, scoringOrder, index);
			AttackType attackType = attackClassifier.classify(event.rule().category());
			int threatScore = threatScoreCalculator.calculate(
					event.rule().severity(),
					event.action(),
					event.path(),
					repeatOffender);
			entities.add(toEntity(event, attackType, threatScore, receivedAt));
		}

		repository.saveAll(entities);
		repository.flush();

		List<String> ingestedIds = events.stream().map(SecurityEventRequest::eventId).toList();
		return new IngestionResponse(entities.size(), ingestedIds);
	}

	private void rejectDuplicateIdsWithinBatch(List<SecurityEventRequest> events) {
		Set<String> seen = new LinkedHashSet<>();
		Set<String> duplicates = new LinkedHashSet<>();
		for (SecurityEventRequest event : events) {
			if (!seen.add(event.eventId())) {
				duplicates.add(event.eventId());
			}
		}
		if (!duplicates.isEmpty()) {
			throw new DuplicateEventException(
					"Duplicate event IDs within the same request", List.copyOf(duplicates));
		}
	}

	/**
	 * Counts previously processed same-IP events in the closed event-time window
	 * {@code [timestamp - 10m, timestamp]} using already-stored events plus
	 * earlier events in the stable, timestamp-sorted batch. The current event is
	 * not stored yet and is after the scanned batch prefix, so it never counts
	 * itself. Including it when applying "more than five events" means five prior
	 * events are enough for the current (sixth) event to receive the bonus.
	 * Event time is used (not receipt time) so replayed attack waves are scored
	 * by when they occurred, not how fast they were uploaded.
	 */
	private boolean isRepeatOffender(
			SecurityEventRequest event,
			List<SecurityEventRequest> batch,
			int currentIndex) {
		Instant to = event.timestamp();
		Instant from = to.minus(REPEAT_WINDOW);

		long priorCount = repository.countByClientIpAndTimestampGreaterThanEqualAndTimestampLessThanEqual(
				event.clientIp(), from, to);

		for (int i = 0; i < currentIndex; i++) {
			SecurityEventRequest earlier = batch.get(i);
			if (earlier.clientIp().equals(event.clientIp())
					&& !earlier.timestamp().isBefore(from)
					&& !earlier.timestamp().isAfter(to)) {
				priorCount++;
			}
		}

		return priorCount >= MINIMUM_PRIOR_EVENTS_FOR_REPEAT;
	}

	private SecurityEventEntity toEntity(
			SecurityEventRequest event,
			AttackType attackType,
			int threatScore,
			Instant receivedAt) {
		return new SecurityEventEntity(
				event.eventId(),
				event.timestamp(),
				event.configId(),
				event.policyId(),
				event.clientIp(),
				event.hostname(),
				event.path(),
				event.method(),
				event.statusCode(),
				event.userAgent(),
				event.rule().id(),
				event.rule().name(),
				event.rule().message(),
				event.rule().severity(),
				event.rule().category(),
				event.action(),
				event.geoLocation().country(),
				event.geoLocation().city(),
				event.requestSize(),
				event.responseSize(),
				attackType,
				threatScore,
				receivedAt);
	}
}

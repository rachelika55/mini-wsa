package com.rachelikatz.miniwsa.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.dto.SamplesResponse;
import com.rachelikatz.miniwsa.dto.SecurityEventResponse;
import com.rachelikatz.miniwsa.dto.SecurityEventResponse.GeoLocationResponse;
import com.rachelikatz.miniwsa.dto.SecurityEventResponse.RuleResponse;
import com.rachelikatz.miniwsa.exception.InvalidRequestException;
import com.rachelikatz.miniwsa.persistence.entity.SecurityEventEntity;
import com.rachelikatz.miniwsa.persistence.repository.SampleFilter;
import com.rachelikatz.miniwsa.persistence.repository.SampleQueryResult;
import com.rachelikatz.miniwsa.persistence.repository.SecurityEventRepository;

@Service
public class SamplesService {

	static final int DEFAULT_LIMIT = 20;
	static final int MAX_LIMIT = 100;

	private final SecurityEventRepository repository;

	public SamplesService(SecurityEventRepository repository) {
		this.repository = repository;
	}

	/**
	 * Returns enriched events matching the (all optional) filters, newest first,
	 * with limit/offset paging and the total number of matches. Runs read-only and
	 * repeatable-read so the page and its count come from one consistent snapshot.
	 */
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public SamplesResponse getSamples(
			Long configId,
			Instant from,
			Instant to,
			RuleCategory category,
			EventAction action,
			Integer limit,
			Integer offset) {

		if (from != null && to != null && !from.isBefore(to)) {
			throw new InvalidRequestException("'from' must be strictly before 'to'");
		}

		int resolvedLimit = resolveLimit(limit);
		int resolvedOffset = resolveOffset(offset);

		SampleFilter filter = new SampleFilter(configId, from, to, category, action, resolvedLimit, resolvedOffset);
		SampleQueryResult result = repository.findSamples(filter);

		List<SecurityEventResponse> items = result.events().stream()
				.map(SamplesService::toResponse)
				.toList();

		return new SamplesResponse(items, result.totalCount(), resolvedLimit, resolvedOffset);
	}

	private int resolveLimit(Integer limit) {
		if (limit == null) {
			return DEFAULT_LIMIT;
		}
		if (limit < 1 || limit > MAX_LIMIT) {
			throw new InvalidRequestException("'limit' must be between 1 and " + MAX_LIMIT);
		}
		return limit;
	}

	private int resolveOffset(Integer offset) {
		if (offset == null) {
			return 0;
		}
		if (offset < 0) {
			throw new InvalidRequestException("'offset' must be zero or greater");
		}
		return offset;
	}

	private static SecurityEventResponse toResponse(SecurityEventEntity e) {
		return new SecurityEventResponse(
				e.getEventId(),
				e.getTimestamp(),
				e.getConfigId(),
				e.getPolicyId(),
				e.getClientIp(),
				e.getHostname(),
				e.getPath(),
				e.getMethod(),
				e.getStatusCode(),
				e.getUserAgent(),
				new RuleResponse(
						e.getRuleId(),
						e.getRuleName(),
						e.getRuleMessage(),
						e.getSeverity(),
						e.getRuleCategory()),
				e.getAction(),
				new GeoLocationResponse(e.getCountry(), e.getCity()),
				e.getRequestSize(),
				e.getResponseSize(),
				e.getAttackType().displayValue(),
				e.getThreatScore(),
				e.getReceivedAt());
	}
}

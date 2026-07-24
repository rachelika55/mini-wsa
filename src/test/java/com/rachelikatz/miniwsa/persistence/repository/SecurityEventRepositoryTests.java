package com.rachelikatz.miniwsa.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.rachelikatz.miniwsa.domain.AttackType;
import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.domain.Severity;
import com.rachelikatz.miniwsa.persistence.entity.SecurityEventEntity;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class SecurityEventRepositoryTests {

	@Autowired
	private SecurityEventRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void savesAndReadsCompleteEnrichedEvent() {
		SecurityEventEntity event = completeEvent(
				"evt-00132",
				"203.0.113.42",
				Instant.parse("2026-05-20T14:32:10Z"));

		repository.saveAndFlush(event);
		entityManager.clear();

		SecurityEventEntity stored = repository.findById("evt-00132").orElseThrow();

		assertThat(stored.getEventId()).isEqualTo("evt-00132");
		assertThat(stored.getConfigId()).isEqualTo(14227L);
		assertThat(stored.getPolicyId()).isEqualTo("pol_web1");
		assertThat(stored.getClientIp()).isEqualTo("203.0.113.42");
		assertThat(stored.getHostname()).isEqualTo("www.example.com");
		assertThat(stored.getPath()).isEqualTo("/api/v1/login");
		assertThat(stored.getMethod()).isEqualTo("POST");
		assertThat(stored.getStatusCode()).isEqualTo(403);
		assertThat(stored.getUserAgent()).isEqualTo("Mozilla/5.0");
		assertThat(stored.getRuleId()).isEqualTo("950001");
		assertThat(stored.getRuleName()).isEqualTo("SQL_INJECTION");
		assertThat(stored.getRuleMessage()).isEqualTo("SQL Injection Attack Detected");
		assertThat(stored.getCountry()).isEqualTo("CN");
		assertThat(stored.getCity()).isEqualTo("Beijing");
		assertThat(stored.getRequestSize()).isEqualTo(1024L);
		assertThat(stored.getResponseSize()).isEqualTo(256L);
		assertThat(stored.getThreatScore()).isEqualTo(90);
	}

	@Test
	void preservesEnumsAndInstants() {
		Instant eventTimestamp = Instant.parse("2026-05-20T14:32:10Z");
		SecurityEventEntity event = completeEvent("evt-enums", "203.0.113.42", eventTimestamp);

		repository.saveAndFlush(event);
		entityManager.clear();

		SecurityEventEntity stored = repository.findById("evt-enums").orElseThrow();

		assertThat(stored.getTimestamp()).isEqualTo(eventTimestamp);
		assertThat(stored.getReceivedAt()).isEqualTo(Instant.parse("2026-05-20T14:32:11Z"));
		assertThat(stored.getSeverity()).isEqualTo(Severity.CRITICAL);
		assertThat(stored.getRuleCategory()).isEqualTo(RuleCategory.INJECTION);
		assertThat(stored.getAction()).isEqualTo(EventAction.DENY);
		assertThat(stored.getAttackType()).isEqualTo(AttackType.SQL_COMMAND_INJECTION);
	}

	@Test
	void countsOneClientIpInsideInclusiveExclusiveTimeWindow() {
		repository.save(completeEvent(
				"evt-start",
				"203.0.113.42",
				Instant.parse("2026-05-20T14:00:00Z")));
		repository.save(completeEvent(
				"evt-middle",
				"203.0.113.42",
				Instant.parse("2026-05-20T14:05:00Z")));
		repository.save(completeEvent(
				"evt-other-ip",
				"198.51.100.10",
				Instant.parse("2026-05-20T14:05:00Z")));
		repository.flush();

		long count = repository.countByClientIpAndTimestampGreaterThanEqualAndTimestampLessThan(
				"203.0.113.42",
				Instant.parse("2026-05-20T14:00:00Z"),
				Instant.parse("2026-05-20T14:10:00Z"));

		assertThat(count).isEqualTo(2);
	}

	@Test
	void excludesEventsOutsideInclusiveExclusiveTimeWindow() {
		repository.save(completeEvent(
				"evt-before",
				"203.0.113.42",
				Instant.parse("2026-05-20T13:59:59Z")));
		repository.save(completeEvent(
				"evt-at-end",
				"203.0.113.42",
				Instant.parse("2026-05-20T14:10:00Z")));
		repository.flush();

		long count = repository.countByClientIpAndTimestampGreaterThanEqualAndTimestampLessThan(
				"203.0.113.42",
				Instant.parse("2026-05-20T14:00:00Z"),
				Instant.parse("2026-05-20T14:10:00Z"));

		assertThat(count).isZero();
	}

	@Test
	void rejectsDuplicateEventId() {
		repository.saveAndFlush(completeEvent(
				"evt-duplicate",
				"203.0.113.42",
				Instant.parse("2026-05-20T14:00:00Z")));
		entityManager.clear();

		assertThatThrownBy(() -> repository.saveAndFlush(completeEvent(
				"evt-duplicate",
				"198.51.100.10",
				Instant.parse("2026-05-20T14:01:00Z"))))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private SecurityEventEntity completeEvent(String eventId, String clientIp, Instant timestamp) {
		return new SecurityEventEntity(
				eventId,
				timestamp,
				14227L,
				"pol_web1",
				clientIp,
				"www.example.com",
				"/api/v1/login",
				"POST",
				403,
				"Mozilla/5.0",
				"950001",
				"SQL_INJECTION",
				"SQL Injection Attack Detected",
				Severity.CRITICAL,
				RuleCategory.INJECTION,
				EventAction.DENY,
				"CN",
				"Beijing",
				1024L,
				256L,
				AttackType.SQL_COMMAND_INJECTION,
				90,
				Instant.parse("2026-05-20T14:32:11Z"));
	}
}

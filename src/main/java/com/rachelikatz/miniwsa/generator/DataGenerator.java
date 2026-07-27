package com.rachelikatz.miniwsa.generator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.domain.Severity;
import com.rachelikatz.miniwsa.generator.GeneratedEvent.GeneratedGeoLocation;
import com.rachelikatz.miniwsa.generator.GeneratedEvent.GeneratedRule;

/**
 * Produces realistic-looking security events for load/demo testing. Output is a
 * deterministic function of the seed and end time, so the same inputs always
 * yield the same dataset (useful for reproducible demos and tests).
 *
 * <p>The stream mixes two shapes of traffic:
 * <ul>
 *   <li><b>Attack waves</b>: bursts from a single client IP hitting a single
 *       path in a sub-10-minute window. Waves large enough (&gt; 6 events)
 *       exercise the repeat-offender bonus and dominate the top-attackers and
 *       top-paths statistics.</li>
 *   <li><b>Background noise</b>: isolated events with varied IPs, paths,
 *       categories and actions, so aggregates are not perfectly clean.</li>
 * </ul>
 */
public class DataGenerator {

	private static final double WAVE_PROBABILITY = 0.55;
	private static final int MIN_WAVE_SIZE = 7;
	private static final int MAX_WAVE_SIZE = 60;
	private static final Duration MAX_WAVE_SPAN = Duration.ofMinutes(9);

	private static final long[] CONFIG_IDS = {14227L, 88011L, 99999L};
	private static final String[] POLICY_IDS = {"pol_web1", "pol_api", "pol_edge"};
	private static final String[] HOSTNAMES = {"www.example.com", "api.example.com", "shop.example.com"};
	private static final String[] METHODS = {"GET", "POST", "PUT", "DELETE"};
	private static final String[] BROWSER_AGENTS = {
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
			"Mozilla/5.0 (X11; Linux x86_64)"
	};
	private static final String[] BOT_AGENTS = {
			"curl/8.4.0", "python-requests/2.31.0", "sqlmap/1.8", "Googlebot/2.1"
	};
	private static final GeneratedGeoLocation[] GEOS = {
			new GeneratedGeoLocation("CN", "Beijing"),
			new GeneratedGeoLocation("RU", "Moscow"),
			new GeneratedGeoLocation("US", "Ashburn"),
			new GeneratedGeoLocation("BR", "Sao Paulo"),
			new GeneratedGeoLocation("IN", "Mumbai"),
			new GeneratedGeoLocation("DE", "Frankfurt")
	};

	private static final List<Scenario> SCENARIOS = List.of(
			new Scenario(RuleCategory.INJECTION, "950001", "SQL_INJECTION",
					"SQL Injection Attack Detected",
					List.of(Severity.CRITICAL, Severity.HIGH),
					List.of(EventAction.DENY),
					List.of("/api/v1/login", "/admin/login", "/search"), 403),
			new Scenario(RuleCategory.XSS, "941100", "XSS_ATTACK",
					"Cross-Site Scripting Attempt",
					List.of(Severity.HIGH, Severity.MEDIUM),
					List.of(EventAction.DENY, EventAction.ALERT),
					List.of("/search", "/comments", "/profile"), 403),
			new Scenario(RuleCategory.PROTOCOL_VIOLATION, "920100", "PROTOCOL_ANOMALY",
					"Malformed HTTP Request",
					List.of(Severity.MEDIUM, Severity.LOW),
					List.of(EventAction.ALERT, EventAction.MONITOR),
					List.of("/", "/api/v1/orders"), 400),
			new Scenario(RuleCategory.DATA_LEAKAGE, "970001", "DATA_LEAK",
					"Sensitive Data Exposure",
					List.of(Severity.HIGH, Severity.MEDIUM),
					List.of(EventAction.ALERT, EventAction.DENY),
					List.of("/api/v1/users", "/export", "/admin/reports"), 200),
			new Scenario(RuleCategory.BOT, "990001", "BOT_DETECTED",
					"Automated Bot Activity",
					List.of(Severity.LOW),
					List.of(EventAction.MONITOR, EventAction.ALERT),
					List.of("/robots.txt", "/product", "/api/v1/catalog"), 200),
			new Scenario(RuleCategory.DOS, "930001", "DOS_PATTERN",
					"Denial of Service Pattern",
					List.of(Severity.HIGH, Severity.CRITICAL),
					List.of(EventAction.DENY),
					List.of("/api/v1/search", "/checkout"), 503),
			new Scenario(RuleCategory.RATE_LIMIT, "960001", "RATE_LIMIT",
					"Rate Limit Exceeded",
					List.of(Severity.LOW, Severity.MEDIUM),
					List.of(EventAction.ALERT, EventAction.MONITOR),
					List.of("/api/v1/login", "/api/v1/token"), 429));

	private final Random random;
	private int sequence;

	public DataGenerator(long seed) {
		this.random = new Random(seed);
	}

	/**
	 * Generates exactly {@code totalEvents} events whose timestamps fall within
	 * {@code [endTime - span, endTime]}.
	 */
	public List<GeneratedEvent> generate(int totalEvents, Duration span, Instant endTime) {
		if (totalEvents < 0) {
			throw new IllegalArgumentException("totalEvents must not be negative");
		}
		if (span.isNegative()) {
			throw new IllegalArgumentException("span must not be negative");
		}
		sequence = 0;
		Instant rangeStart = endTime.minus(span);
		Duration waveSpan = span.compareTo(MAX_WAVE_SPAN) < 0 ? span : MAX_WAVE_SPAN;
		List<GeneratedEvent> events = new ArrayList<>(totalEvents);

		while (events.size() < totalEvents) {
			int remaining = totalEvents - events.size();
			if (remaining >= MIN_WAVE_SIZE && random.nextDouble() < WAVE_PROBABILITY) {
				generateWave(events, remaining, rangeStart, endTime, waveSpan);
			} else {
				events.add(generateNoise(rangeStart, endTime));
			}
		}
		return events;
	}

	private void generateWave(
			List<GeneratedEvent> events,
			int remaining,
			Instant rangeStart,
			Instant endTime,
			Duration waveSpan) {
		int waveSize = Math.min(remaining, MIN_WAVE_SIZE + random.nextInt(MAX_WAVE_SIZE - MIN_WAVE_SIZE + 1));

		Scenario scenario = pick(SCENARIOS);
		String attackerIp = randomIp();
		long configId = pick(CONFIG_IDS);
		String policyId = pick(POLICY_IDS);
		String hostname = pick(HOSTNAMES);
		String path = pick(scenario.paths());
		GeneratedGeoLocation geo = pick(GEOS);
		String userAgent = scenario.category() == RuleCategory.BOT ? pick(BOT_AGENTS) : pick(BROWSER_AGENTS);

		Instant waveStart = randomInstantBetween(rangeStart, endTime.minus(waveSpan));
		long spanSeconds = waveSpan.getSeconds();

		for (int i = 0; i < waveSize; i++) {
			Instant timestamp = waveStart.plusSeconds((long) (random.nextDouble() * spanSeconds));
			events.add(buildEvent(scenario, configId, policyId, attackerIp, hostname, path, geo, userAgent, timestamp));
		}
	}

	private GeneratedEvent generateNoise(Instant rangeStart, Instant endTime) {
		Scenario scenario = pick(SCENARIOS);
		String userAgent = scenario.category() == RuleCategory.BOT ? pick(BOT_AGENTS) : pick(BROWSER_AGENTS);
		Instant timestamp = randomInstantBetween(rangeStart, endTime);
		return buildEvent(
				scenario,
				pick(CONFIG_IDS),
				pick(POLICY_IDS),
				randomIp(),
				pick(HOSTNAMES),
				pick(scenario.paths()),
				pick(GEOS),
				userAgent,
				timestamp);
	}

	private GeneratedEvent buildEvent(
			Scenario scenario,
			long configId,
			String policyId,
			String clientIp,
			String hostname,
			String path,
			GeneratedGeoLocation geo,
			String userAgent,
			Instant timestamp) {
		Severity severity = pick(scenario.severities());
		EventAction action = pick(scenario.actions());
		String eventId = "evt-" + String.format("%08d", ++sequence);

		GeneratedRule rule = new GeneratedRule(
				scenario.ruleId(),
				scenario.ruleName(),
				scenario.ruleMessage(),
				severity.name(),
				scenario.category().name());

		return new GeneratedEvent(
				eventId,
				timestamp.toString(),
				configId,
				policyId,
				clientIp,
				hostname,
				path,
				pick(METHODS),
				scenario.statusCode(),
				userAgent,
				rule,
				action.name(),
				geo,
				256L + random.nextInt(8192),
				256L + random.nextInt(65536));
	}

	private String randomIp() {
		int block = random.nextInt(3);
		String prefix = switch (block) {
			case 0 -> "203.0.113.";
			case 1 -> "198.51.100.";
			default -> "192.0.2.";
		};
		return prefix + (1 + random.nextInt(254));
	}

	private Instant randomInstantBetween(Instant start, Instant end) {
		long startSeconds = start.getEpochSecond();
		long endSeconds = Math.max(startSeconds, end.getEpochSecond());
		long offset = startSeconds == endSeconds ? 0 : (long) (random.nextDouble() * (endSeconds - startSeconds));
		return Instant.ofEpochSecond(startSeconds + offset);
	}

	private <T> T pick(List<T> values) {
		return values.get(random.nextInt(values.size()));
	}

	private <T> T pick(T[] values) {
		return values[random.nextInt(values.length)];
	}

	private long pick(long[] values) {
		return values[random.nextInt(values.length)];
	}

	private record Scenario(
			RuleCategory category,
			String ruleId,
			String ruleName,
			String ruleMessage,
			List<Severity> severities,
			List<EventAction> actions,
			List<String> paths,
			int statusCode) {
	}
}

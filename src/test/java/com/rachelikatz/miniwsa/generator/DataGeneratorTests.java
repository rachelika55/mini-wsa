package com.rachelikatz.miniwsa.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.domain.Severity;

class DataGeneratorTests {

	private static final Instant END = Instant.parse("2026-05-20T15:00:00Z");
	private static final Duration SPAN = Duration.ofMinutes(1_440);

	@Test
	void generatesExactlyTheRequestedCount() {
		List<GeneratedEvent> events = new DataGenerator(42).generate(1_000, SPAN, END);
		assertThat(events).hasSize(1_000);
	}

	@Test
	void isDeterministicForAGivenSeed() {
		List<GeneratedEvent> first = new DataGenerator(7).generate(500, SPAN, END);
		List<GeneratedEvent> second = new DataGenerator(7).generate(500, SPAN, END);
		assertThat(first).isEqualTo(second);
	}

	@Test
	void assignsUniqueEventIds() {
		List<GeneratedEvent> events = new DataGenerator(1).generate(2_000, SPAN, END);
		Set<String> ids = events.stream().map(GeneratedEvent::eventId).collect(Collectors.toSet());
		assertThat(ids).hasSize(events.size());
	}

	@Test
	void emitsOnlyValidEnumAndCategoryValues() {
		List<GeneratedEvent> events = new DataGenerator(3).generate(1_000, SPAN, END);
		for (GeneratedEvent event : events) {
			assertThat(EnumSet.allOf(EventAction.class)).extracting(Enum::name).contains(event.action());
			assertThat(EnumSet.allOf(RuleCategory.class)).extracting(Enum::name).contains(event.rule().category());
			assertThat(EnumSet.allOf(Severity.class)).extracting(Enum::name).contains(event.rule().severity());
		}
	}

	@Test
	void keepsTimestampsWithinTheRequestedWindowAndParseable() {
		Instant start = END.minus(SPAN);
		List<GeneratedEvent> events = new DataGenerator(9).generate(1_000, SPAN, END);
		for (GeneratedEvent event : events) {
			Instant timestamp = Instant.parse(event.timestamp());
			assertThat(timestamp).isBetween(start, END);
		}
	}

	@Test
	void emitsEventsInChronologicalOrder() {
		List<GeneratedEvent> events = new DataGenerator(9).generate(1_000, SPAN, END);

		assertThat(events)
				.extracting(GeneratedEvent::timestamp)
				.map(Instant::parse)
				.isSorted();
	}

	@Test
	void keepsAttackWavesInsideASpanShorterThanTheMaximumWaveDuration() {
		Duration shortSpan = Duration.ofMinutes(1);
		Instant start = END.minus(shortSpan);

		List<GeneratedEvent> events = new DataGenerator(42).generate(1_000, shortSpan, END);

		assertThat(events)
				.extracting(GeneratedEvent::timestamp)
				.map(Instant::parse)
				.allSatisfy(timestamp -> assertThat(timestamp).isBetween(start, END));
	}

	@Test
	void rejectsNegativeSpan() {
		assertThatThrownBy(() -> new DataGenerator(42).generate(10, Duration.ofMinutes(-1), END))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("span must not be negative");
	}

	@Test
	void producesAtLeastOneRepeatOffenderWave() {
		List<GeneratedEvent> events = new DataGenerator(42).generate(2_000, SPAN, END);

		Map<String, List<Instant>> timestampsByIp = new HashMap<>();
		for (GeneratedEvent event : events) {
			timestampsByIp
					.computeIfAbsent(event.clientIp(), ignored -> new java.util.ArrayList<>())
					.add(Instant.parse(event.timestamp()));
		}

		boolean repeatOffenderExists = timestampsByIp.values().stream()
				.anyMatch(DataGeneratorTests::hasMoreThanFiveWithinTenMinutes);
		assertThat(repeatOffenderExists).isTrue();
	}

	private static boolean hasMoreThanFiveWithinTenMinutes(List<Instant> timestamps) {
		if (timestamps.size() <= 5) {
			return false;
		}
		TreeMap<Instant, Integer> sorted = new TreeMap<>();
		for (Instant timestamp : timestamps) {
			sorted.merge(timestamp, 1, Integer::sum);
		}
		for (Instant anchor : sorted.keySet()) {
			int count = sorted.subMap(anchor, true, anchor.plus(Duration.ofMinutes(10)), true).values()
					.stream().mapToInt(Integer::intValue).sum();
			if (count > 5) {
				return true;
			}
		}
		return false;
	}
}

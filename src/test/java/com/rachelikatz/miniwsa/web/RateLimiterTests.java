package com.rachelikatz.miniwsa.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RateLimiterTests {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-05-20T14:00:00Z"));

	@Test
	void allowsUpToTheLimitThenRejects() {
		RateLimiter limiter = new RateLimiter(3, clock);

		assertThat(limiter.tryAcquire("ip-1")).isTrue();
		assertThat(limiter.tryAcquire("ip-1")).isTrue();
		assertThat(limiter.tryAcquire("ip-1")).isTrue();
		assertThat(limiter.tryAcquire("ip-1")).isFalse();
	}

	@Test
	void tracksKeysIndependently() {
		RateLimiter limiter = new RateLimiter(1, clock);

		assertThat(limiter.tryAcquire("ip-1")).isTrue();
		assertThat(limiter.tryAcquire("ip-2")).isTrue();
		assertThat(limiter.tryAcquire("ip-1")).isFalse();
	}

	@Test
	void resetsAfterTheWindowElapses() {
		RateLimiter limiter = new RateLimiter(1, clock);

		assertThat(limiter.tryAcquire("ip-1")).isTrue();
		assertThat(limiter.tryAcquire("ip-1")).isFalse();

		clock.advance(Duration.ofSeconds(61));

		assertThat(limiter.tryAcquire("ip-1")).isTrue();
	}

	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		private void advance(Duration duration) {
			this.instant = this.instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}

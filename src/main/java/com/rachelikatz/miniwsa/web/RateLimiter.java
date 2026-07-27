package com.rachelikatz.miniwsa.web;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window, per-key rate limiter. Each key (client IP) is allowed
 * {@code maxPerMinute} requests within a fixed one-minute window; the window
 * resets on the first request after it elapses.
 *
 * <p>Thread-safe via {@link ConcurrentHashMap#compute}. This is intentionally an
 * in-process limiter; a multi-instance deployment would use a shared store
 * (e.g. Redis) so the limit is enforced across all nodes.
 */
public class RateLimiter {

	private static final long WINDOW_MILLIS = 60_000L;

	private final int maxPerMinute;
	private final Clock clock;
	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	public RateLimiter(int maxPerMinute, Clock clock) {
		this.maxPerMinute = maxPerMinute;
		this.clock = clock;
	}

	/** Records a request for {@code key} and returns {@code true} if it is within the limit. */
	public boolean tryAcquire(String key) {
		long now = clock.millis();
		Window updated = windows.compute(key, (ignored, existing) -> {
			if (existing == null || now - existing.startMillis() >= WINDOW_MILLIS) {
				return new Window(now, 1);
			}
			return new Window(existing.startMillis(), existing.count() + 1);
		});
		return updated.count() <= maxPerMinute;
	}

	private record Window(long startMillis, int count) {
	}
}

package com.rachelikatz.miniwsa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-client-IP rate limiting for the read APIs (stats and samples). When a
 * client exceeds {@code requestsPerMinute} within a fixed one-minute window the
 * request is rejected with 429.
 */
@ConfigurationProperties(prefix = "miniwsa.ratelimit")
public class RateLimitProperties {

	private boolean enabled = true;
	private int requestsPerMinute = 100;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getRequestsPerMinute() {
		return requestsPerMinute;
	}

	public void setRequestsPerMinute(int requestsPerMinute) {
		this.requestsPerMinute = requestsPerMinute;
	}
}

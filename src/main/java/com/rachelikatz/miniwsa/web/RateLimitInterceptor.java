package com.rachelikatz.miniwsa.web;

import org.springframework.web.servlet.HandlerInterceptor;

import com.rachelikatz.miniwsa.exception.RateLimitExceededException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rejects requests from a client IP that exceeds the configured rate with a 429.
 * Registered only on the read APIs (stats and samples).
 */
public class RateLimitInterceptor implements HandlerInterceptor {

	private final RateLimiter rateLimiter;
	private final boolean enabled;

	public RateLimitInterceptor(RateLimiter rateLimiter, boolean enabled) {
		this.rateLimiter = rateLimiter;
		this.enabled = enabled;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!enabled) {
			return true;
		}
		String clientKey = request.getRemoteAddr();
		if (!rateLimiter.tryAcquire(clientKey)) {
			throw new RateLimitExceededException("Rate limit exceeded; try again later");
		}
		return true;
	}
}

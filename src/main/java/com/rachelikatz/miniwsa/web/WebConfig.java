package com.rachelikatz.miniwsa.web;

import java.time.Clock;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.rachelikatz.miniwsa.config.RateLimitProperties;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final RateLimitProperties properties;
	private final Clock clock;

	public WebConfig(RateLimitProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		RateLimiter rateLimiter = new RateLimiter(properties.getRequestsPerMinute(), clock);
		registry.addInterceptor(new RateLimitInterceptor(rateLimiter, properties.isEnabled()))
				.addPathPatterns("/v1/stats/**", "/v1/events/samples");
	}
}

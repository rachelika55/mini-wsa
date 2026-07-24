package com.rachelikatz.miniwsa.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rachelikatz.miniwsa.enrichment.AttackClassifier;
import com.rachelikatz.miniwsa.enrichment.ThreatScoreCalculator;

@Configuration
public class MiniWsaConfiguration {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public AttackClassifier attackClassifier() {
		return new AttackClassifier();
	}

	@Bean
	public ThreatScoreCalculator threatScoreCalculator() {
		return new ThreatScoreCalculator();
	}
}

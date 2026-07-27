package com.rachelikatz.miniwsa.config;

import java.time.Clock;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rachelikatz.miniwsa.enrichment.AttackClassifier;
import com.rachelikatz.miniwsa.enrichment.ThreatScoreCalculator;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration
@EnableConfigurationProperties({IngestionProperties.class, RateLimitProperties.class})
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

	@Bean
	public JsonMapperBuilderCustomizer strictIngestionJson() {
		return builder -> builder
				.enable(
						DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
						DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
				.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
				.disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
				.withCoercionConfig(LogicalType.DateTime, config -> {
					config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
					config.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
				});
	}
}

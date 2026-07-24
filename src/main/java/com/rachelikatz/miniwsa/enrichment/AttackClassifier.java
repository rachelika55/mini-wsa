package com.rachelikatz.miniwsa.enrichment;

import com.rachelikatz.miniwsa.domain.AttackType;
import com.rachelikatz.miniwsa.domain.RuleCategory;

public class AttackClassifier {

	public AttackType classify(RuleCategory category) {
		return switch (category) {
			case INJECTION -> AttackType.SQL_COMMAND_INJECTION;
			case XSS -> AttackType.CROSS_SITE_SCRIPTING;
			case PROTOCOL_VIOLATION -> AttackType.PROTOCOL_ANOMALY;
			case DATA_LEAKAGE -> AttackType.DATA_EXFILTRATION;
			case BOT -> AttackType.BOT_ACTIVITY;
			case DOS -> AttackType.DENIAL_OF_SERVICE;
			case RATE_LIMIT -> AttackType.RATE_LIMITING;
		};
	}
}

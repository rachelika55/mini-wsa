package com.rachelikatz.miniwsa.domain;

public enum AttackType {
	SQL_COMMAND_INJECTION("SQL/Command Injection"),
	CROSS_SITE_SCRIPTING("Cross-Site Scripting"),
	PROTOCOL_ANOMALY("Protocol Anomaly"),
	DATA_EXFILTRATION("Data Exfiltration"),
	BOT_ACTIVITY("Bot Activity"),
	DENIAL_OF_SERVICE("Denial of Service"),
	RATE_LIMITING("Rate Limiting");

	private final String displayValue;

	AttackType(String displayValue) {
		this.displayValue = displayValue;
	}

	public String displayValue() {
		return displayValue;
	}
}

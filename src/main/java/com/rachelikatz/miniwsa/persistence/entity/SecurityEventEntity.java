package com.rachelikatz.miniwsa.persistence.entity;

import java.time.Instant;

import com.rachelikatz.miniwsa.domain.AttackType;
import com.rachelikatz.miniwsa.domain.EventAction;
import com.rachelikatz.miniwsa.domain.RuleCategory;
import com.rachelikatz.miniwsa.domain.Severity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "security_events")
public class SecurityEventEntity implements Persistable<String> {

	@Id
	@Column(name = "event_id", nullable = false, updatable = false)
	private String eventId;

	@Column(name = "event_timestamp", nullable = false)
	private Instant timestamp;

	@Column(name = "config_id", nullable = false)
	private long configId;

	@Column(name = "policy_id", nullable = false)
	private String policyId;

	@Column(name = "client_ip", nullable = false)
	private String clientIp;

	@Column(nullable = false)
	private String hostname;

	@Column(nullable = false, length = 2048)
	private String path;

	@Column(nullable = false)
	private String method;

	@Column(name = "status_code", nullable = false)
	private int statusCode;

	@Column(name = "user_agent", nullable = false, length = 2048)
	private String userAgent;

	@Column(name = "rule_id", nullable = false)
	private String ruleId;

	@Column(name = "rule_name", nullable = false)
	private String ruleName;

	@Column(name = "rule_message", nullable = false, length = 2048)
	private String ruleMessage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Severity severity;

	@Enumerated(EnumType.STRING)
	@Column(name = "rule_category", nullable = false)
	private RuleCategory ruleCategory;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventAction action;

	@Column(nullable = false)
	private String country;

	@Column(nullable = false)
	private String city;

	@Column(name = "request_size", nullable = false)
	private long requestSize;

	@Column(name = "response_size", nullable = false)
	private long responseSize;

	@Enumerated(EnumType.STRING)
	@Column(name = "attack_type", nullable = false)
	private AttackType attackType;

	@Column(name = "threat_score", nullable = false)
	private int threatScore;

	@Column(name = "received_at", nullable = false)
	private Instant receivedAt;

	@Transient
	private boolean newEntity = true;

	protected SecurityEventEntity() {
	}

	public SecurityEventEntity(
			String eventId,
			Instant timestamp,
			long configId,
			String policyId,
			String clientIp,
			String hostname,
			String path,
			String method,
			int statusCode,
			String userAgent,
			String ruleId,
			String ruleName,
			String ruleMessage,
			Severity severity,
			RuleCategory ruleCategory,
			EventAction action,
			String country,
			String city,
			long requestSize,
			long responseSize,
			AttackType attackType,
			int threatScore,
			Instant receivedAt) {
		this.eventId = eventId;
		this.timestamp = timestamp;
		this.configId = configId;
		this.policyId = policyId;
		this.clientIp = clientIp;
		this.hostname = hostname;
		this.path = path;
		this.method = method;
		this.statusCode = statusCode;
		this.userAgent = userAgent;
		this.ruleId = ruleId;
		this.ruleName = ruleName;
		this.ruleMessage = ruleMessage;
		this.severity = severity;
		this.ruleCategory = ruleCategory;
		this.action = action;
		this.country = country;
		this.city = city;
		this.requestSize = requestSize;
		this.responseSize = responseSize;
		this.attackType = attackType;
		this.threatScore = threatScore;
		this.receivedAt = receivedAt;
	}

	public String getEventId() {
		return eventId;
	}

	@Override
	public String getId() {
		return eventId;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	@PostLoad
	@PostPersist
	private void markNotNew() {
		newEntity = false;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public long getConfigId() {
		return configId;
	}

	public String getPolicyId() {
		return policyId;
	}

	public String getClientIp() {
		return clientIp;
	}

	public String getHostname() {
		return hostname;
	}

	public String getPath() {
		return path;
	}

	public String getMethod() {
		return method;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public String getRuleId() {
		return ruleId;
	}

	public String getRuleName() {
		return ruleName;
	}

	public String getRuleMessage() {
		return ruleMessage;
	}

	public Severity getSeverity() {
		return severity;
	}

	public RuleCategory getRuleCategory() {
		return ruleCategory;
	}

	public EventAction getAction() {
		return action;
	}

	public String getCountry() {
		return country;
	}

	public String getCity() {
		return city;
	}

	public long getRequestSize() {
		return requestSize;
	}

	public long getResponseSize() {
		return responseSize;
	}

	public AttackType getAttackType() {
		return attackType;
	}

	public int getThreatScore() {
		return threatScore;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}
}

CREATE TABLE security_events (
    event_id VARCHAR(255) NOT NULL,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    config_id BIGINT NOT NULL,
    policy_id VARCHAR(255) NOT NULL,
    client_ip VARCHAR(255) NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    path VARCHAR(2048) NOT NULL,
    method VARCHAR(255) NOT NULL,
    status_code INTEGER NOT NULL,
    user_agent VARCHAR(2048) NOT NULL,
    rule_id VARCHAR(255) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_message VARCHAR(2048) NOT NULL,
    severity VARCHAR(255) NOT NULL,
    rule_category VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    request_size BIGINT NOT NULL,
    response_size BIGINT NOT NULL,
    attack_type VARCHAR(255) NOT NULL,
    threat_score INTEGER NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_security_events PRIMARY KEY (event_id)
);

-- Repeat-offender counting: same client IP within a short event-time window.
CREATE INDEX idx_security_events_client_ip_timestamp
    ON security_events (client_ip, event_timestamp);

-- Config-scoped statistics and samples over a time range.
CREATE INDEX idx_security_events_config_timestamp
    ON security_events (config_id, event_timestamp);

-- All-config time filtering and newest-first samples ordering.
CREATE INDEX idx_security_events_timestamp_event_id
    ON security_events (event_timestamp, event_id);

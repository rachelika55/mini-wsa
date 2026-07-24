package com.rachelikatz.miniwsa.persistence.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rachelikatz.miniwsa.persistence.entity.SecurityEventEntity;

public interface SecurityEventRepository extends JpaRepository<SecurityEventEntity, String> {

	long countByClientIpAndTimestampGreaterThanEqualAndTimestampLessThan(
			String clientIp,
			Instant fromInclusive,
			Instant toExclusive);
}

package com.rachelikatz.miniwsa.persistence.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rachelikatz.miniwsa.persistence.entity.SecurityEventEntity;
import com.rachelikatz.miniwsa.persistence.projection.ActionAggregate;
import com.rachelikatz.miniwsa.persistence.projection.AttackerAggregate;
import com.rachelikatz.miniwsa.persistence.projection.CategoryAggregate;
import com.rachelikatz.miniwsa.persistence.projection.PathAggregate;

public interface SecurityEventRepository extends JpaRepository<SecurityEventEntity, String> {

	long countByClientIpAndTimestampGreaterThanEqualAndTimestampLessThan(
			String clientIp,
			Instant fromInclusive,
			Instant toExclusive);

	@Query("""
			select count(e) from SecurityEventEntity e
			where (:configId is null or e.configId = :configId)
			  and e.timestamp >= :from
			  and e.timestamp < :to
			""")
	long countSummary(
			@Param("configId") Long configId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	@Query("""
			select new com.rachelikatz.miniwsa.persistence.projection.CategoryAggregate(
			    e.ruleCategory, count(e), avg(e.threatScore))
			from SecurityEventEntity e
			where (:configId is null or e.configId = :configId)
			  and e.timestamp >= :from
			  and e.timestamp < :to
			group by e.ruleCategory
			""")
	List<CategoryAggregate> aggregateByCategory(
			@Param("configId") Long configId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	@Query("""
			select new com.rachelikatz.miniwsa.persistence.projection.ActionAggregate(
			    e.action, count(e))
			from SecurityEventEntity e
			where (:configId is null or e.configId = :configId)
			  and e.timestamp >= :from
			  and e.timestamp < :to
			group by e.action
			""")
	List<ActionAggregate> aggregateByAction(
			@Param("configId") Long configId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	@Query("""
			select new com.rachelikatz.miniwsa.persistence.projection.AttackerAggregate(
			    e.clientIp, count(e), avg(e.threatScore))
			from SecurityEventEntity e
			where (:configId is null or e.configId = :configId)
			  and e.timestamp >= :from
			  and e.timestamp < :to
			group by e.clientIp
			order by count(e) desc, e.clientIp asc
			""")
	List<AttackerAggregate> topAttackers(
			@Param("configId") Long configId,
			@Param("from") Instant from,
			@Param("to") Instant to,
			Pageable pageable);

	@Query("""
			select new com.rachelikatz.miniwsa.persistence.projection.PathAggregate(
			    e.path, count(e))
			from SecurityEventEntity e
			where (:configId is null or e.configId = :configId)
			  and e.timestamp >= :from
			  and e.timestamp < :to
			group by e.path
			order by count(e) desc, e.path asc
			""")
	List<PathAggregate> topTargetedPaths(
			@Param("configId") Long configId,
			@Param("from") Instant from,
			@Param("to") Instant to,
			Pageable pageable);
}

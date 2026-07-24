package com.rachelikatz.miniwsa.persistence.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.rachelikatz.miniwsa.persistence.entity.SecurityEventEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class SecurityEventSampleQueryRepositoryImpl implements SecurityEventSampleQueryRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public SampleQueryResult findSamples(SampleFilter filter) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		CriteriaQuery<SecurityEventEntity> dataQuery = cb.createQuery(SecurityEventEntity.class);
		Root<SecurityEventEntity> dataRoot = dataQuery.from(SecurityEventEntity.class);
		dataQuery.select(dataRoot)
				.where(predicates(cb, dataRoot, filter))
				.orderBy(cb.desc(dataRoot.get("timestamp")), cb.asc(dataRoot.get("eventId")));

		List<SecurityEventEntity> events = entityManager.createQuery(dataQuery)
				.setFirstResult(filter.offset())
				.setMaxResults(filter.limit())
				.getResultList();

		CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
		Root<SecurityEventEntity> countRoot = countQuery.from(SecurityEventEntity.class);
		countQuery.select(cb.count(countRoot))
				.where(predicates(cb, countRoot, filter));

		long totalCount = entityManager.createQuery(countQuery).getSingleResult();

		return new SampleQueryResult(events, totalCount);
	}

	private Predicate[] predicates(CriteriaBuilder cb, Root<SecurityEventEntity> root, SampleFilter filter) {
		List<Predicate> predicates = new ArrayList<>();
		if (filter.configId() != null) {
			predicates.add(cb.equal(root.get("configId"), filter.configId()));
		}
		if (filter.from() != null) {
			predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("timestamp"), filter.from()));
		}
		if (filter.to() != null) {
			predicates.add(cb.lessThan(root.<Instant>get("timestamp"), filter.to()));
		}
		if (filter.category() != null) {
			predicates.add(cb.equal(root.get("ruleCategory"), filter.category()));
		}
		if (filter.action() != null) {
			predicates.add(cb.equal(root.get("action"), filter.action()));
		}
		return predicates.toArray(new Predicate[0]);
	}
}

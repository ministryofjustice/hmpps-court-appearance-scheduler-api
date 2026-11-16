package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.ConcurrentRequestRejectedException
import java.util.UUID
import java.util.UUID.nameUUIDFromBytes

interface FindIdsRepository {
  fun findIds(spec: Specification<CourtAppearance>, pageable: Pageable): Page<UUID>
}

@Transactional(readOnly = true, timeout = 2)
class FindIdsRepositoryImpl : FindIdsRepository {
  @PersistenceContext
  internal lateinit var entityManager: EntityManager

  override fun findIds(spec: Specification<CourtAppearance>, pageable: Pageable): Page<UUID> {
    acquireLock()

    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(UUID::class.java)
    val root = query.from(CourtAppearance::class.java)
    query.select(root.get(CourtAppearance::id.name))
      .where(spec.toPredicate(root, query, cb))

    query.orderBy(cb.asc(root.get<UUID>(CourtAppearance::id.name)))

    val typedQuery = entityManager.createQuery(query)
    if (pageable.isPaged) {
      typedQuery.firstResult = pageable.offset.toInt()
      typedQuery.maxResults = pageable.pageSize
    }

    val ids = typedQuery.resultList

    return PageableExecutionUtils.getPage(ids, pageable) {
      countMatchingAppearances(spec)
    }
  }

  private fun acquireLock() {
    val lockAcquired = entityManager
      .createNativeQuery("SELECT pg_try_advisory_xact_lock(:lockId)")
      .setParameter("lockId", LOCK_ID)
      .singleResult as Boolean

    if (!lockAcquired) {
      throw ConcurrentRequestRejectedException("Unable to acquire lock to find ids")
    }
  }

  private fun countMatchingAppearances(spec: Specification<CourtAppearance>): Long {
    val cb = entityManager.criteriaBuilder
    val countQuery = cb.createQuery(Long::class.java)
    val countRoot = countQuery.from(CourtAppearance::class.java)
    countQuery.select(cb.count(countRoot))
      .where(spec.toPredicate(countRoot, countQuery, cb))
    return entityManager.createQuery(countQuery).singleResult
  }

  companion object {
    private val LOCK_ID = nameUUIDFromBytes("/schedule-replication/court-appearance-ids".toByteArray()).mostSignificantBits
  }
}

package com.acantilado.core.amenity;

import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.core.amenity.fields.GoogleAmenityStatus;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class GoogleAmenitySnapshotDAO extends AbstractDAO<GoogleAmenitySnapshot> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAmenitySnapshotDAO.class);

    public GoogleAmenitySnapshotDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public GoogleAmenitySnapshot save(GoogleAmenitySnapshot snapshot) {
        return persist(snapshot);
    }

    public GoogleAmenitySnapshot update(GoogleAmenitySnapshot snapshot) {
        return (GoogleAmenitySnapshot) currentSession().merge(snapshot);
    }

    public GoogleAmenitySnapshot saveOrUpdate(GoogleAmenitySnapshot snapshot) {
        currentSession().saveOrUpdate(snapshot);
        return snapshot;
    }

    /**
     * Find the most recent snapshot for a given place ID
     * This is the most commonly used query for checking if data has changed
     */
    public Optional<GoogleAmenitySnapshot> findLatestByPlaceId(String placeId) {
        List<GoogleAmenitySnapshot> results = currentSession()
                .createQuery(
                        "SELECT s FROM GoogleAmenitySnapshot s " +
                                "WHERE s.placeId = :placeId " +
                                "ORDER BY s.lastSeen DESC",
                        GoogleAmenitySnapshot.class)
                .setParameter("placeId", placeId)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find all snapshots for a given place ID, ordered by lastSeen DESC
     * Used for migration and historical analysis
     */
    public List<GoogleAmenitySnapshot> findAllByPlaceId(String placeId) {
        return currentSession()
                .createQuery(
                        "SELECT s FROM GoogleAmenitySnapshot s " +
                                "WHERE s.placeId = :placeId " +
                                "ORDER BY s.lastSeen DESC",
                        GoogleAmenitySnapshot.class)
                .setParameter("placeId", placeId)
                .getResultList();
    }

    /**
     * Find snapshots within a time range for a given place ID
     * Useful for analyzing changes over a specific period
     */
    public List<GoogleAmenitySnapshot> findByPlaceIdAndTimeRange(String placeId,
                                                                 Instant startTime,
                                                                 Instant endTime) {
        return currentSession()
                .createQuery(
                        "SELECT s FROM GoogleAmenitySnapshot s " +
                                "WHERE s.placeId = :placeId " +
                                "AND s.lastSeen BETWEEN :startTime AND :endTime " +
                                "ORDER BY s.lastSeen DESC",
                        GoogleAmenitySnapshot.class)
                .setParameter("placeId", placeId)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * Find all snapshots for a given chain within a time range
     * Useful for analyzing chain-wide trends
     */
    public List<GoogleAmenitySnapshot> findByChainAndTimeRange(AcantiladoAmenityChain chain,
                                                               Instant startTime,
                                                               Instant endTime) {
        return currentSession()
                .createQuery(
                        "SELECT s FROM GoogleAmenitySnapshot s " +
                                "JOIN GoogleAmenity a ON s.placeId = a.placeId " +
                                "WHERE a.chain = :chain " +
                                "AND s.lastSeen BETWEEN :startTime AND :endTime " +
                                "ORDER BY s.lastSeen DESC",
                        GoogleAmenitySnapshot.class)
                .setParameter("chain", chain)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * Find snapshots where status changed to a specific status
     * Useful for detecting closures or reopenings
     */
    public List<GoogleAmenitySnapshot> findByStatusChange(GoogleAmenityStatus status,
                                                          Instant since) {
        return currentSession()
                .createQuery(
                        "SELECT s FROM GoogleAmenitySnapshot s " +
                                "WHERE s.status = :status " +
                                "AND s.firstSeen >= :since " +
                                "ORDER BY s.firstSeen DESC",
                        GoogleAmenitySnapshot.class)
                .setParameter("status", status)
                .setParameter("since", since)
                .getResultList();
    }

    /**
     * Find amenities that haven't been seen since a certain time
     * Useful for detecting potentially closed or missing amenities
     */
    public List<GoogleAmenitySnapshot> findStaleSnapshots(Instant notSeenSince) {
        return currentSession()
                .createQuery(
                        "SELECT s FROM GoogleAmenitySnapshot s " +
                                "WHERE s.lastSeen < :notSeenSince " +
                                "AND s.id IN (" +
                                "  SELECT MAX(s2.id) FROM GoogleAmenitySnapshot s2 " +
                                "  GROUP BY s2.placeId" +
                                ") " +
                                "ORDER BY s.lastSeen DESC",
                        GoogleAmenitySnapshot.class)
                .setParameter("notSeenSince", notSeenSince)
                .getResultList();
    }

    /**
     * Count snapshots for a given place ID
     */
    public long countByPlaceId(String placeId) {
        return currentSession()
                .createQuery(
                        "SELECT COUNT(s) FROM GoogleAmenitySnapshot s WHERE s.placeId = :placeId",
                        Long.class)
                .setParameter("placeId", placeId)
                .getSingleResult();
    }

    /**
     * Delete all snapshots for a given place ID
     * Used during place ID migration
     */
    public int deleteByPlaceId(String placeId) {
        return currentSession()
                .createQuery("DELETE FROM GoogleAmenitySnapshot s WHERE s.placeId = :placeId")
                .setParameter("placeId", placeId)
                .executeUpdate();
    }

    /**
     * Find snapshots with rating changes greater than a threshold
     * Useful for detecting significant quality changes
     */
    public List<GoogleAmenitySnapshot> findSignificantRatingChanges(double minChange,
                                                                    Instant since) {
        // This is a more complex query that requires comparing consecutive snapshots
        String hql =
                "SELECT s1 FROM GoogleAmenitySnapshot s1, GoogleAmenitySnapshot s2 " +
                        "WHERE s1.placeId = s2.placeId " +
                        "AND s1.firstSeen > s2.lastSeen " +
                        "AND s1.firstSeen >= :since " +
                        "AND ABS(s1.rating - s2.rating) >= :minChange " +
                        "AND s2.id = (" +
                        "  SELECT MAX(s3.id) FROM GoogleAmenitySnapshot s3 " +
                        "  WHERE s3.placeId = s1.placeId " +
                        "  AND s3.lastSeen < s1.firstSeen" +
                        ") " +
                        "ORDER BY s1.firstSeen DESC";

        return currentSession()
                .createQuery(hql, GoogleAmenitySnapshot.class)
                .setParameter("since", since)
                .setParameter("minChange", minChange)
                .getResultList();
    }

    /**
     * Find all snapshots (for batch processing)
     */
    public List<GoogleAmenitySnapshot> findAll() {
        return list(namedTypedQuery("com.acantilado.core.amenity.GoogleAmenitySnapshot.findAll"));
    }

    /**
     * Count all snapshots
     */
    public long countAll() {
        return currentSession()
                .createQuery("SELECT COUNT(s) FROM GoogleAmenitySnapshot s", Long.class)
                .getSingleResult();
    }
}
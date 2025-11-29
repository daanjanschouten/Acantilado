package com.acantilado.core.amenity;

import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.core.amenity.fields.AcantiladoAmenityType;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class GoogleAmenityDAO extends AbstractDAO<GoogleAmenity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAmenityDAO.class);

    public GoogleAmenityDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<GoogleAmenity> findByPlaceId(String placeId) {
        return Optional.ofNullable(get(placeId));
    }

    public GoogleAmenity merge(GoogleAmenity amenity) {
        return (GoogleAmenity) currentSession().merge(amenity);
    }

    public GoogleAmenity saveOrUpdate(GoogleAmenity amenity) {
        currentSession().saveOrUpdate(amenity);
        return amenity;
    }

    public void delete(String placeId) {
        Optional<GoogleAmenity> amenity = findByPlaceId(placeId);
        amenity.ifPresent(a -> currentSession().delete(a));
    }

    /**
     * Find amenities by chain
     */
    public List<GoogleAmenity> findByChain(AcantiladoAmenityChain chain) {
        return namedTypedQuery("com.acantilado.core.amenity.GoogleAmenity.findByChain")
                .setParameter("chain", chain)
                .getResultList();
    }

    /**
     * Find amenities by type (derived from chain)
     */
    public List<GoogleAmenity> findByType(AcantiladoAmenityType type) {
        return currentSession()
                .createQuery("SELECT a FROM GoogleAmenity a WHERE a.chain.amenityType = :type", GoogleAmenity.class)
                .setParameter("type", type)
                .getResultList();
    }

    /**
     * Find amenities within a bounding box and optionally filter by chain
     * Used for detecting potential Place ID changes
     */
    public List<GoogleAmenity> findByLocationAndChain(double latitude,
                                                      double longitude,
                                                      double delta,
                                                      AcantiladoAmenityChain chain) {
        return currentSession()
                .createQuery(
                        "SELECT a FROM GoogleAmenity a " +
                                "WHERE a.latitude BETWEEN :minLat AND :maxLat " +
                                "AND a.longitude BETWEEN :minLon AND :maxLon " +
                                "AND a.chain = :chain",
                        GoogleAmenity.class)
                .setParameter("minLat", latitude - delta)
                .setParameter("maxLat", latitude + delta)
                .setParameter("minLon", longitude - delta)
                .setParameter("maxLon", longitude + delta)
                .setParameter("chain", chain)
                .getResultList();
    }

    /**
     * Find amenities within a bounding box (any chain)
     */
    public List<GoogleAmenity> findByLocation(double latitude,
                                              double longitude,
                                              double delta) {
        return currentSession()
                .createQuery(
                        "SELECT a FROM GoogleAmenity a " +
                                "WHERE a.latitude BETWEEN :minLat AND :maxLat " +
                                "AND a.longitude BETWEEN :minLon AND :maxLon",
                        GoogleAmenity.class)
                .setParameter("minLat", latitude - delta)
                .setParameter("maxLat", latitude + delta)
                .setParameter("minLon", longitude - delta)
                .setParameter("maxLon", longitude + delta)
                .getResultList();
    }

    /**
     * Find amenity by previous Place ID (for migration tracking)
     */
    public Optional<GoogleAmenity> findByPreviousPlaceId(String previousPlaceId) {
        List<GoogleAmenity> results = currentSession()
                .createQuery("SELECT a FROM GoogleAmenity a WHERE a.previousPlaceId = :previousPlaceId", GoogleAmenity.class)
                .setParameter("previousPlaceId", previousPlaceId)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find all amenities (for batch processing)
     */
    public List<GoogleAmenity> findAll() {
        return list(namedTypedQuery("com.acantilado.core.amenity.GoogleAmenity.findAll"));
    }

    /**
     * Count amenities by chain
     */
    public long countByChain(AcantiladoAmenityChain chain) {
        return currentSession()
                .createQuery("SELECT COUNT(a) FROM GoogleAmenity a WHERE a.chain = :chain", Long.class)
                .setParameter("chain", chain)
                .getSingleResult();
    }

    /**
     * Count all amenities
     */
    public long countAll() {
        return currentSession()
                .createQuery("SELECT COUNT(a) FROM GoogleAmenity a", Long.class)
                .getSingleResult();
    }
}
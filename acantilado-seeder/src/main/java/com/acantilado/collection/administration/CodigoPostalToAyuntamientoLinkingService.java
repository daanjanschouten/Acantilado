package com.acantilado.collection.administration;

import com.acantilado.core.administrative.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class CodigoPostalToAyuntamientoLinkingService extends CollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodigoPostalToAyuntamientoLinkingService.class);
    private static final double PROXIMITY_DEGREES_THRESHOLD = 0.005;

    private final AyuntamientoDAO ayuntamientoDAO;
    private final CodigoPostalDAO codigoPostalDAO;
    private final SessionFactory sessionFactory;

    public CodigoPostalToAyuntamientoLinkingService(AyuntamientoDAO ayuntamientoDAO,
                                                    CodigoPostalDAO codigoPostalDAO,
                                                    SessionFactory sessionFactory) {
        this.ayuntamientoDAO = ayuntamientoDAO;
        this.codigoPostalDAO = codigoPostalDAO;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public boolean isSeedingNecessary() {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);

        try {
            return codigoPostalDAO.findByAyuntamiento("28079").isEmpty();
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    @Override
    public void seed() {
        LOGGER.info("Linking  postal codes to ayuntamientos");
        collectPostCodesByProvinceIds(sessionFactory, codigoPostalDAO)
                .forEach((provinciaId, postalCodes)
                        -> linkPostCodesToAyuntamientos(postalCodes, provinciaId));
        LOGGER.info("Finished linking postal codes to ayuntamientos");
    }

    /**
     * Linking is based only on proximity, not intersection. This is because these links are purely used to filter
     * down possible postcodes for a given real estate listing, as Idealista provides a reliable ayuntamiento.
     */
    private void linkPostCodesToAyuntamientos(Set<String> postalCodeIds, String provinciaId) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        int totalLinksCreated = 0;

        try {
            ManagedSessionContext.bind(session);
            transaction = session.beginTransaction();

            List<Ayuntamiento> ayuntamientos = ayuntamientoDAO.findByProvinceId(provinciaId);
            LOGGER.info("Linking {} postcodes to {} ayuntamientos for provincia {}",
                    postalCodeIds.size(),
                    ayuntamientos.size(),
                    provinciaId);

            for (String postalCodeId : postalCodeIds) {
                int linksForThisPostalCode = 0;
                List<CodigoPostal> codigosPostales = getCodigosPostales(postalCodeId, codigoPostalDAO);

                for (CodigoPostal cp : codigosPostales) {
                    for (Ayuntamiento ayuntamiento : ayuntamientos) {
                        try {
                            double distance = cp.getGeometry().distance(ayuntamiento.getGeometry());
                            if (distance > PROXIMITY_DEGREES_THRESHOLD) {
                                continue;
                            }

                            cp.getAyuntamientos().add(ayuntamiento);
                            ayuntamiento.getCodigosPostales().add(cp);
                            linksForThisPostalCode++;
                        } catch (Exception e) {
                            LOGGER.error("Error linking postal code {} to municipality {} ({})",
                                    postalCodeId, ayuntamiento.getName(), ayuntamiento.getId(), e);
                        }
                    }
                }

                if (linksForThisPostalCode == 0) {
                    LOGGER.error("No ayuntamientos found for postal code {} at proximity setting {}",
                            postalCodeId, PROXIMITY_DEGREES_THRESHOLD);
                    // logClosestAyuntamiento(codigosPostales.get(0), ayuntamientos);
                }
                totalLinksCreated += linksForThisPostalCode;
            }

            session.flush();
            transaction.commit();

            double averageLinkCount = !postalCodeIds.isEmpty() ? (double) totalLinksCreated / postalCodeIds.size() : 0;
            LOGGER.debug("Created {} links with {} links on average", totalLinksCreated, averageLinkCount);
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Failed to link postal codes to municipalities", e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    private void logClosestAyuntamiento(CodigoPostal codigoPostal, List<Ayuntamiento> ayuntamientos) {
        double minDistance = Double.MAX_VALUE;
        String closestAyuntamiento = "unknown";

        for (Ayuntamiento ayuntamiento : ayuntamientos) {
            double distance = codigoPostal.getGeometry().distance(ayuntamiento.getGeometry());
            if (distance < minDistance) {
                minDistance = distance;
                closestAyuntamiento = ayuntamiento.getName() + " (" + ayuntamiento.getId() + ")";
            }
        }

        throw new RuntimeException("Postal code " + codigoPostal.getCodigoPostal() + " was not linked to any " +
                "municipality. Closest is " + closestAyuntamiento +
                " at distance of " + (int) (minDistance * 111000) + ".");
    }

    private static List<CodigoPostal> getCodigosPostales(String postalCodeId, CodigoPostalDAO codigoPostalDAO) {
        List<CodigoPostal> codigosPostales = codigoPostalDAO.findByCodigoPostal(postalCodeId);
        if (codigosPostales.isEmpty()) {
            throw new RuntimeException("Postal code not found" + postalCodeId);
        }

        return codigosPostales;
    }

    private static String provinciaIdForPostalCode(String postalCodeId) {
        return postalCodeId.substring(0, 2);
    }

    private static Map<String, Set<String>> collectPostCodesByProvinceIds(SessionFactory sessionFactory, CodigoPostalDAO codigoPostalDAO) {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);

        try {
            return codigoPostalDAO.findAll()
                    .stream()
                    .map(CodigoPostal::getCodigoPostal)
                    .collect(Collectors.groupingBy(
                            CodigoPostalToAyuntamientoLinkingService::provinciaIdForPostalCode,
                            Collectors.toSet()
                    ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }
}
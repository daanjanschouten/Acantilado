package location;

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

public final class CodigoPostalToAyuntamientoLinkingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodigoPostalToAyuntamientoLinkingService.class);
    private static final double PROXIMITY_THRESHOLD = 0.005; // ~500 meters

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

    public void link() {
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
    private void linkPostCodesToAyuntamientos(Set<String> postalCodeIds, Long provinciaId) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        int totalLinksCreated = 0;

        try {
            ManagedSessionContext.bind(session);
            transaction = session.beginTransaction();

            List<Ayuntamiento> ayuntamientos = ayuntamientoDAO.findByProvinceId(provinciaId);
            LOGGER.debug("Linking {} postcodes to {} ayuntamientos for provincia {}",
                    postalCodeIds.size(),
                    ayuntamientos.size(),
                    provinciaId);

            for (String postalCodeId : postalCodeIds) {
                int linksForThisPostalCode = 0;
                CodigoPostal cp = getCodigoPostal(postalCodeId, codigoPostalDAO);

                for (Ayuntamiento ayuntamiento : ayuntamientos) {
                    try {
                        double distance = cp.getGeometry().distance(ayuntamiento.getGeometry());
                        if (distance > PROXIMITY_THRESHOLD) {
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

                if (linksForThisPostalCode == 0) {
                    LOGGER.error("No ayuntamientos found for postal code {} at proximity setting {}",
                            postalCodeId, PROXIMITY_THRESHOLD);
                    logClosestAyuntamiento(cp, ayuntamientos);
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

        LOGGER.error("Postal code {} was not linked to any municipality. Closest is {} at distance {} (~{}m)",
                codigoPostal.getCodigoPostal(), closestAyuntamiento, minDistance, (int) (minDistance * 111000));
    }

    private static CodigoPostal getCodigoPostal(String postalCodeId, CodigoPostalDAO codigoPostalDAO) {
        CodigoPostal cp = codigoPostalDAO.findById(postalCodeId).orElse(null);
        if (cp == null) {
            throw new RuntimeException("Postal code not found" + postalCodeId);
        }

        if (cp.getGeometry() == null) {
            throw new RuntimeException("Postal code has no geometry" + postalCodeId);
        }

        return cp;
    }

    private static long provinciaIdForPostalCode(String postalCodeId) {
        return Long.parseLong(postalCodeId.substring(0, 2));
    }

    private static Map<Long, Set<String>> collectPostCodesByProvinceIds(SessionFactory sessionFactory, CodigoPostalDAO codigoPostalDAO) {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);

        try {
            return codigoPostalDAO.findAllIds()
                    .stream()
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
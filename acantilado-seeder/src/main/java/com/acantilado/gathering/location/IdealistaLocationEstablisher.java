package com.acantilado.gathering.location;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.Provincia;
import com.acantilado.core.administrative.ProvinciaDAO;
import com.acantilado.core.idealista.IdealistaLocationDAO;
import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import com.acantilado.gathering.properties.utils.RetryableBatchedExecutor;
import org.hibernate.SessionFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class IdealistaLocationEstablisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaLocationEstablisher.class);

    private final AyuntamientoDAO ayuntamientoDAO;
    private final ProvinciaDAO provinciaDAO;
    private final IdealistaLocationDAO locationDAO;
    private final SessionFactory sessionFactory;


    public IdealistaLocationEstablisher(
            AyuntamientoDAO ayuntamientoDAO,
            ProvinciaDAO provinciaDAO,
            IdealistaLocationDAO locationDAO,
            SessionFactory sessionFactory) {

        this.ayuntamientoDAO = ayuntamientoDAO;
        this.provinciaDAO = provinciaDAO;
        this.locationDAO = locationDAO;
        this.sessionFactory = sessionFactory;
    }

    public String provinceToPopulate(String provinceName) {
        Provincia province = getProvinceFromName(provinciaDAO, sessionFactory, provinceName);
        Set<Long> ayuntamientoIdsForProvince = getAyuntamientoIdsForProvince(ayuntamientoDAO, sessionFactory, province.getId());

        List<IdealistaAyuntamientoLocation> locations = locationDAO.findByProvinceId(province.getId());
        LOGGER.info("Currently {} location codes present for provincia {}, expecting {}", locations.size(), ayuntamientoIdsForProvince.size(), province.getId());
        return province.getIdealistaLocationId();
    }

    private static Provincia getProvinceFromName(ProvinciaDAO provinciaDao, SessionFactory sessionFactory, String name) {
        return RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            List<Provincia> provincias = provinciaDao.findByName(name);
            if (provincias.size() != 1) {
                throw new RuntimeException("More than 1 or 0 provinces found for province " + name);
            }
            return provincias.get(0);
        });
    }

    private static Set<Long> getAyuntamientoIdsForProvince(AyuntamientoDAO ayuntamientoDAO, SessionFactory sessionFactory, long provinceId) {
        return RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction(sessionFactory,
                () -> ayuntamientoDAO.findByProvinceId(provinceId)
                        .stream()
                        .map(Ayuntamiento::getId)
                        .collect(Collectors.toSet()));
    }

    private static Set<Long> findMissingMappings(Set<Long> ayuntamientoIds, Set<Long> mappedAyuntamientoIds) {
        Set<Long> missingIds = new HashSet<>(ayuntamientoIds);
        missingIds.removeAll(mappedAyuntamientoIds);

        Set<Long> extraIds = new HashSet<>(mappedAyuntamientoIds);
        extraIds.removeAll(ayuntamientoIds);

        if (!missingIds.isEmpty() || !extraIds.isEmpty()) {
            if (!missingIds.isEmpty()) {
                LOGGER.error("Missing mappings for ayuntamiento IDs: {}", missingIds);
                return missingIds;
            }

            LOGGER.error("Unexpected mappings for ayuntamiento IDs: {}", extraIds);
            throw new RuntimeException();
        }
        return Set.of();
    }

    private Ayuntamiento establishAyuntamientoByCoordinates(Point locationPoint) {
        List<Ayuntamiento> allAyuntamientos = ayuntamientoDAO.findAll();

        for (Ayuntamiento ayuntamiento : allAyuntamientos) {
            if (ayuntamiento.getGeometry().contains(locationPoint)) {
                LOGGER.debug("Point is inside ayuntamiento {}", ayuntamiento.getName());
                return ayuntamiento;
            }
        }

        LOGGER.debug("Point not contained in any ayuntamiento, using closest match");
        Optional<Ayuntamiento> maybeClosestAyuntamiento = allAyuntamientos.stream()
                .min(Comparator.comparingDouble(a -> a.getGeometry().distance(locationPoint)))
                .map(closest -> {
                    LOGGER.debug("Selecting closest ayuntamiento {}", closest.getName());
                    return closest;
                });

        return maybeClosestAyuntamiento.orElseThrow();
    }

}

package com.acantilado.gathering.location;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.IdealistaLocationMapping;
import com.acantilado.core.administrative.IdealistaLocationMappingDAO;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class NovelLocationEstablisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(NovelLocationEstablisher.class);

    private final IdealistaLocationMappingDAO mappingDAO;
    private final Set<Ayuntamiento> ayuntamientosForProvince;
    private final BiFunction<Ayuntamiento, Point, Optional<AcantiladoLocation>> locationProducer;

    private final Map<Long, Map<IdealistaLocationMapping, Integer>> ayuntamientoIdsByCountOfIdealistaLocationIds = new HashMap<>();

    public NovelLocationEstablisher(
            IdealistaLocationMappingDAO mappingDAO,
            Set<Ayuntamiento> ayuntamientosForProvince,
            BiFunction<Ayuntamiento, Point, Optional<AcantiladoLocation>> locationProducer) {

        this.mappingDAO = mappingDAO;
        this.ayuntamientosForProvince = ayuntamientosForProvince;
        this.locationProducer = locationProducer;
    }

    public Optional<AcantiladoLocation> establish(
            String idealistaAyuntamiento,
            String normalizedIdealistaLocationId,
            Point locationPoint) {

        Ayuntamiento ayuntamiento = establishAyuntamientoByCoordinates(locationPoint);
        IdealistaLocationMapping mapping = new IdealistaLocationMapping(
                normalizedIdealistaLocationId,
                idealistaAyuntamiento,
                ayuntamiento.getId(),
                ayuntamiento.getName());

        updateInMemoryMapping(ayuntamiento.getId(), mapping);
        return locationProducer.apply(ayuntamiento, locationPoint);
    }

    public void storeInMemoryMappings() {
        Set<IdealistaLocationMapping> locationMappings = new HashSet<>();

        ayuntamientoIdsByCountOfIdealistaLocationIds.forEach((ayuntamientoId, countsByLocation) -> {
            if (countsByLocation.size() == 1) {
                countsByLocation.forEach((location, count) -> {
                    location.setConfidenceScore(count);
                    locationMappings.add(location);
                    LOGGER.debug("SUCCESS: found a single entry for ayuntamiento ID {}: {} with {} hits",
                            ayuntamientoId, location, count);
                });
            } else {
                countsByLocation.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .ifPresent(maxEntry -> {
                            IdealistaLocationMapping winningLocation = maxEntry.getKey();
                            int winningCount = maxEntry.getValue();

                            winningLocation.setConfidenceScore(winningCount);
                            locationMappings.add(winningLocation);

                            countsByLocation.forEach((location, count) -> {
                                if (!location.equals(winningLocation)) {
                                    LOGGER.debug("  Rejected: {} ({} properties)", location, count);
                                }
                            });
                        });
            }
        });

        LOGGER.info("Final in-memory mapping: {}.", locationMappings);

        AtomicInteger mappingsStored = new AtomicInteger();
        locationMappings.forEach(mapping -> {
            List<IdealistaLocationMapping> existingMappings = mappingDAO.findByAyuntamientoId(mapping.getAcantiladoAyuntamientoId());
            if (!existingMappings.isEmpty()) {
                LOGGER.info("Found an existing mapping {} for mapping {}, skipping", existingMappings, mapping);
            } else if (mapping.getConfidenceScore() < 5) {
                LOGGER.info("Property count is insufficient for storage for {} -> {} {} with score {}",
                        mapping.getAcantiladoAyuntamientoId(),
                        mapping.getIdealistaLocationId(),
                        mapping.getIdealistaMunicipalityName(),
                        mapping.getConfidenceScore());
            } else {
                mappingDAO.saveOrUpdate(mapping);
                LOGGER.info("Created new mapping: {} {} → {} ({}) with score {}",
                        mapping.getAcantiladoAyuntamientoId(),
                        mapping.getAcantiladoMunicipalityName(),
                        mapping.getIdealistaMunicipalityName(),
                        mapping.getIdealistaLocationId(),
                        mapping.getConfidenceScore());
                mappingsStored.incrementAndGet();
            }
        });

        LOGGER.info("Finished storing {} mappings", mappingsStored.get());
    }


    private Ayuntamiento establishAyuntamientoByCoordinates(Point locationPoint) {
        for (Ayuntamiento ayuntamiento : ayuntamientosForProvince) {
            if (ayuntamiento.getGeometry().contains(locationPoint)) {
                LOGGER.debug("Point is inside ayuntamiento {}", ayuntamiento.getName());
                return ayuntamiento;
            }
        }

        LOGGER.debug("Point not contained in any ayuntamiento, using closest match");
        Optional<Ayuntamiento> maybeClosestAyuntamiento = ayuntamientosForProvince.stream()
                .min(Comparator.comparingDouble(a -> a.getGeometry().distance(locationPoint)))
                .map(closest -> {
                    LOGGER.info("Selecting closest ayuntamiento {} for {}", closest.getName(), locationPoint);
                    return closest;
                });

        return maybeClosestAyuntamiento.orElseThrow();
    }

    private void updateInMemoryMapping(Long ayuntamientoId, IdealistaLocationMapping mapping) {
        if (ayuntamientoIdsByCountOfIdealistaLocationIds.containsKey(ayuntamientoId)) {
            Map<IdealistaLocationMapping, Integer> locationIdByCounts = ayuntamientoIdsByCountOfIdealistaLocationIds.get(ayuntamientoId);
            if (locationIdByCounts.containsKey(mapping)) {
                locationIdByCounts.compute(mapping, (k, existingCount) -> existingCount + 1);
            } else {
                locationIdByCounts.put(mapping, 1);
            }
        } else {
            Map<IdealistaLocationMapping, Integer> newMap = new HashMap<>();
            newMap.put(mapping, 1);
            ayuntamientoIdsByCountOfIdealistaLocationIds.put(ayuntamientoId, newMap);
        }
    }
}

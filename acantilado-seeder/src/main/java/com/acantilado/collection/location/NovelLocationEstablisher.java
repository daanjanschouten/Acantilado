package com.acantilado.collection.location;

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
    private final BiFunction<Ayuntamiento, Point, AcantiladoLocation> locationProducer;

    private final Map<IdealistaLocationMapping, Integer> mappingsByCounts = new HashMap<>();

    public NovelLocationEstablisher(
            IdealistaLocationMappingDAO mappingDAO,
            Set<Ayuntamiento> ayuntamientosForProvince,
            BiFunction<Ayuntamiento, Point, AcantiladoLocation> locationProducer) {

        this.mappingDAO = mappingDAO;
        this.ayuntamientosForProvince = ayuntamientosForProvince;
        this.locationProducer = locationProducer;
    }

    public AcantiladoLocation establish(String idealistaAyuntamiento, String locationId, Point locationPoint) {
        Ayuntamiento ayuntamiento = establishAyuntamientoByCoordinates(locationPoint);
        IdealistaLocationMapping mapping = new IdealistaLocationMapping(
                locationId,
                idealistaAyuntamiento,
                ayuntamiento.getId(),
                ayuntamiento.getName());

        updateInMemoryMapping(mapping);
        return locationProducer.apply(ayuntamiento, locationPoint);
    }

    public void storeInMemoryMappings() {
        AtomicInteger mappingsStored = new AtomicInteger();
        mappingsByCounts.forEach((mapping, count) -> {
            List<IdealistaLocationMapping> existingMappings = mappingDAO.findByAyuntamientoId(mapping.getAcantiladoAyuntamientoId());
            if (existingMappings.contains(mapping)) {
                LOGGER.debug("Mapping {} already existed, skipping", mapping);
                return;
            }

            LOGGER.info("Created new mapping: {} with score {}", mapping, count);
            mappingsStored.incrementAndGet();
            mappingDAO.saveOrUpdate(mapping);
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

        Optional<Ayuntamiento> maybeClosestAyuntamiento = ayuntamientosForProvince.stream()
                .min(Comparator.comparingDouble(a -> a.getGeometry().distance(locationPoint)))
                .map(closest -> {
                    LOGGER.debug("Selecting closest ayuntamiento {} for location {}", closest.getName(), locationPoint);
                    return closest;
                });

        return maybeClosestAyuntamiento.orElseThrow();
    }

    private void updateInMemoryMapping(IdealistaLocationMapping mapping) {
        if (mappingsByCounts.containsKey(mapping)) {
            mappingsByCounts.compute(mapping, (k, existingCount) -> existingCount + 1);
        } else {
            mappingsByCounts.put(mapping, 1);
        }
    }
}
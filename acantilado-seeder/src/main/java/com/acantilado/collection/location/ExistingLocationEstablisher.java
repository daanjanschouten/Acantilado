package com.acantilado.collection.location;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.IdealistaLocationMapping;
import com.acantilado.core.administrative.IdealistaLocationMappingDAO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ExistingLocationEstablisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingLocationEstablisher.class);
    private static final double MAX_ACCEPTABLE_DISTANCE_DEGREES = 0.01;

    private final AyuntamientoDAO ayuntamientoDAO;
    private final IdealistaLocationMappingDAO mappingDAO;
    private final BiFunction<Ayuntamiento, Point, AcantiladoLocation> locationProducer;

    private final Cache<String, List<IdealistaLocationMapping>> mappingCache;
    private final Cache<String, Ayuntamiento> ayuntamientoCache;

    public ExistingLocationEstablisher(
            AyuntamientoDAO ayuntamientoDAO,
            IdealistaLocationMappingDAO mappingDAO,
            BiFunction<Ayuntamiento, Point, AcantiladoLocation> locationProducer) {
        this.ayuntamientoDAO = ayuntamientoDAO;
        this.mappingDAO = mappingDAO;
        this.locationProducer = locationProducer;

        this.mappingCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();

        this.ayuntamientoCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build();
    }

    protected AcantiladoLocation establish(String locationId, Point locationPoint) {
        return locationProducer.apply(
                establishAyuntamiento(locationId, locationPoint),
                locationPoint
        );
    }

    private Ayuntamiento establishAyuntamiento(String locationId, Point locationPoint) {
        // Cache lookup for mappings
        List<IdealistaLocationMapping> mappingsForLocationId = mappingCache.get(
                locationId,
                key -> mappingDAO.findByIdealistaLocationId(key)
        );

        if (mappingsForLocationId.isEmpty()) {
            LOGGER.error("No mapping found for location ID {}", locationId);
            throw new IllegalStateException();
        }

        if (mappingsForLocationId.size() > 1) {
            Set<String> ayuntamientoIds = mappingsForLocationId
                    .stream()
                    .map(IdealistaLocationMapping::getAcantiladoAyuntamientoId)
                    .collect(Collectors.toSet());

            return selectFromAyuntamientos(ayuntamientoIds, locationId, locationPoint);
        }

        String ayuntamientoId = mappingsForLocationId.get(0).getAcantiladoAyuntamientoId();

        return ayuntamientoCache.get(ayuntamientoId, key ->
                ayuntamientoDAO.findById(key).orElseThrow()
        );
    }

    private Ayuntamiento selectFromAyuntamientos(Set<String> ayuntamientoIds, String locationId, Point locationPoint) {
        Set<Ayuntamiento> ayuntamientos = ayuntamientoIds.stream()
                .map(id -> ayuntamientoCache.get(id, key ->
                        ayuntamientoDAO.findById(key).orElseThrow()))
                .collect(Collectors.toSet());

        Optional<Ayuntamiento> maybeAyuntamiento = ayuntamientos.stream()
                .filter(a -> a.getGeometry().contains(locationPoint))
                .findFirst();

        if (maybeAyuntamiento.isPresent()) {
            return maybeAyuntamiento.get();
        }

        Optional<Ayuntamiento> maybeClosestAyuntamiento = ayuntamientos.stream()
                .min(Comparator.comparingDouble(a -> a.getGeometry().distance(locationPoint)));

        if (maybeClosestAyuntamiento.isPresent()) {
            Ayuntamiento closest = maybeClosestAyuntamiento.get();
            double distance = closest.getGeometry().distance(locationPoint);

            if (distance < MAX_ACCEPTABLE_DISTANCE_DEGREES) {
                LOGGER.debug("Point slightly outside polygon for location {}. Using closest ayuntamiento {} (distance: {})",
                        locationId, closest.getName(), distance);
            } else {
                LOGGER.error("Point {} very far from any mapped ayuntamiento for location {}. " +
                                "Using closest ayuntamiento {} but distance is {} degrees (~{}km). " +
                                "Reseed this location ID.",
                        locationId, locationPoint, closest.getName(), distance, (int)(distance * 111));
            }
            return closest;
        }

        throw new IllegalStateException("No ayuntamientos found for location " + locationId);
    }
}
package com.acantilado.collection.location;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.IdealistaLocationMapping;
import com.acantilado.core.administrative.IdealistaLocationMappingDAO;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ExistingLocationEstablisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingLocationEstablisher.class);

    private final AyuntamientoDAO ayuntamientoDAO;
    private final IdealistaLocationMappingDAO mappingDAO;
    private final BiFunction<Ayuntamiento, Point, Optional<AcantiladoLocation>> locationProducer;

    public ExistingLocationEstablisher(
            AyuntamientoDAO ayuntamientoDAO,
            IdealistaLocationMappingDAO mappingDAO,
            BiFunction<Ayuntamiento, Point, Optional<AcantiladoLocation>> locationProducer) {

        this.ayuntamientoDAO = ayuntamientoDAO;
        this.mappingDAO = mappingDAO;

        this.locationProducer = locationProducer;
    }

    protected Optional<AcantiladoLocation> establish(String locationId, Point locationPoint) {
        return locationProducer.apply(
                establishAyuntamiento(locationId, locationPoint),
                locationPoint
        );
    }

    private Ayuntamiento establishAyuntamiento(String locationId, Point locationPoint) {
        List<IdealistaLocationMapping> mappingsForLocationId = mappingDAO.findByIdealistaLocationId(locationId);
        if (mappingsForLocationId.isEmpty()) {
            LOGGER.error("No mapping found for location ID {}", locationId);
            throw new IllegalStateException();
        }

        if (mappingsForLocationId.size() > 1) {
            Set<Long> ayuntamientoIds = mappingsForLocationId
                    .stream()
                    .map(IdealistaLocationMapping::getAcantiladoAyuntamientoId)
                    .collect(Collectors.toSet());

            return selectFromAyuntamientos(ayuntamientoIds, locationPoint);
        }

        long ayuntamientoId = mappingsForLocationId.get(0).getAcantiladoAyuntamientoId();
        return ayuntamientoDAO.findById(ayuntamientoId).orElseThrow();
    }

    private Ayuntamiento selectFromAyuntamientos(Set<Long> ayuntamientoIds, Point locationPoint) {
        Optional<Ayuntamiento> maybeAyuntamiento = ayuntamientoIds.stream()
                .map(ayuntamientoDAO::findById)
                .flatMap(Optional::stream)
                .filter(a -> a.getGeometry().contains(locationPoint))
                .findFirst();

        if (maybeAyuntamiento.isPresent()) {
            return maybeAyuntamiento.get();
        }

        Optional<Ayuntamiento> maybeClosestAyuntamiento = ayuntamientoIds.stream()
                .map(ayuntamientoDAO::findById)
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(a -> a.getGeometry().distance(locationPoint)))
                .map(closest -> {
                    LOGGER.info("Selecting closest ayuntamiento {} for location {}", closest.getName(), locationPoint);
                    return closest;
                });

        return maybeClosestAyuntamiento.orElseThrow();
    }
}

package com.acantilado.collection.location;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.IdealistaLocationMapping;
import com.acantilado.core.administrative.IdealistaLocationMappingDAO;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Optional<Ayuntamiento> maybeAyuntamiento = establishAyuntamiento(locationId, locationPoint);
        return maybeAyuntamiento.flatMap(a -> locationProducer.apply(a, locationPoint));
    }

    private Optional<Ayuntamiento> establishAyuntamiento(String locationId, Point locationPoint) {
        List<IdealistaLocationMapping> mappingsForLocationId = mappingDAO.findByIdealistaLocationId(locationId);
        if (mappingsForLocationId.isEmpty()) {
            LOGGER.error("No mapping found for location ID {}", locationId);
            return Optional.empty();
        }

        if (mappingsForLocationId.size() > 1) {
            Set<Long> ayuntamientoIds = mappingsForLocationId
                    .stream()
                    .map(IdealistaLocationMapping::getAcantiladoAyuntamientoId)
                    .collect(Collectors.toSet());

            Optional<Ayuntamiento> maybeAyuntamiento = selectFromAyuntamientos(ayuntamientoIds, locationPoint);
            LOGGER.info("More than one mapping found for location ID {}", locationId);
            return maybeAyuntamiento;
        }

        long ayuntamientoId = mappingsForLocationId.get(0).getAcantiladoAyuntamientoId();
        return ayuntamientoDAO.findById(ayuntamientoId);
    }

    private Optional<Ayuntamiento> selectFromAyuntamientos(Set<Long> ayuntamientoIds, Point locationPoint) {
        Optional<Ayuntamiento> maybeAyuntamiento = ayuntamientoIds.stream()
                .map(ayuntamientoDAO::findById)
                .flatMap(Optional::stream)
                .filter(a -> a.getGeometry().contains(locationPoint))
                .findFirst();

        maybeAyuntamiento.ifPresentOrElse(
                a -> LOGGER.info("Found point {} inside of ayuntamiento {} based on mappings", locationPoint, a),
                () -> LOGGER.info("No ayuntamiento found among linked mappings {} for location {}", ayuntamientoIds, locationPoint)
        );

        return maybeAyuntamiento;
    }
}

package com.acantilado.gathering.location;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.IdealistaLocationMapping;
import com.acantilado.core.administrative.IdealistaLocationMappingDAO;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

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

    protected Optional<AcantiladoLocation> establish(String normalizedIdealistaLocationId, Point locationPoint) {
        Optional<Ayuntamiento> maybeAyuntamiento = establishAyuntamiento(normalizedIdealistaLocationId);
        return maybeAyuntamiento.flatMap(a -> locationProducer.apply(a, locationPoint));
    }

    private Optional<Ayuntamiento> establishAyuntamiento(String locationId) {
        List<IdealistaLocationMapping> mappingsForLocationId = mappingDAO.findByIdealistaLocationId(locationId);
        if (mappingsForLocationId.isEmpty()) {
            LOGGER.error("No mapping found for location ID {}", locationId);
            return Optional.empty();
        }

        if (mappingsForLocationId.size() > 1) {
            LOGGER.warn("More than one mapping found for location ID {}", locationId);

            // Check which area one this falls into
            return Optional.empty();
        }

        long ayuntamientoId = mappingsForLocationId.get(0).getAcantiladoAyuntamientoId();
        return ayuntamientoDAO.findById(ayuntamientoId);
    }
}

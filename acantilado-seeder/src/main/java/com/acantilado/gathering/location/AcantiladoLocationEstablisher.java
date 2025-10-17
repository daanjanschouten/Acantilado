package com.acantilado.gathering.location;

import com.acantilado.core.administrative.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AcantiladoLocationEstablisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcantiladoLocationEstablisher.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private final AyuntamientoDAO ayuntamientoDAO;
    private final BarrioDAO barrioDAO;
    private final CodigoPostalDAO codigoPostalDAO;

    private final AtomicInteger locationsEstablished = new AtomicInteger();
    private final AtomicInteger bruteForceCodigoPostalMatches = new AtomicInteger();
    private final AtomicInteger barriosEstablished = new AtomicInteger();
    private final AtomicInteger barrioMisses = new AtomicInteger();
    private final Set<String> deduplicatedAyuntamientos = new HashSet<>();

    private static final Map<String, String> AYUNTAMIENTO_MAPPINGS = Map.of(
            "La Font de la Figuera", "la Font de la Figuera",
            "La Lantejuela", "Lantejuela"
    );

    public AcantiladoLocationEstablisher(AyuntamientoDAO ayuntamientoDAO, BarrioDAO barrioDAO, CodigoPostalDAO codigoPostalDAO) {
        this.ayuntamientoDAO = ayuntamientoDAO;
        this.barrioDAO = barrioDAO;
        this.codigoPostalDAO = codigoPostalDAO;
    }

    public AcantiladoLocation establish(String ayuntamientoName, Double latitude, Double longitude, long propertyCode) {
        List<Ayuntamiento> ayuntamientos = ayuntamientoDAO.findByName(ayuntamientoName);
        if (ayuntamientos.isEmpty()) {
            if (AYUNTAMIENTO_MAPPINGS.containsKey(ayuntamientoName)) {
                LOGGER.debug("Fell back to different ayuntamiento name for {}", ayuntamientoName);
                ayuntamientos = ayuntamientoDAO.findByName(AYUNTAMIENTO_MAPPINGS.get(ayuntamientoName));
            } else {
                throw new RuntimeException("No ayuntamiento found with name: " + ayuntamientoName);
            }
        }

        Ayuntamiento ayuntamiento;
        if (ayuntamientos.size() > 1) {
            deduplicatedAyuntamientos.add(ayuntamientoName);
            ayuntamiento = findCorrectAyuntamiento(ayuntamientos, latitude, longitude);
        } else {
            ayuntamiento = ayuntamientos.get(0);
        }

        Point locationPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));

        CodigoPostal codigoPostal = findCodigoPostal(ayuntamiento, locationPoint);
        Barrio barrio = findBarrio(ayuntamiento, locationPoint, propertyCode);

        locationsEstablished.incrementAndGet();
        return Objects.isNull(barrio)
                ? new AcantiladoLocation(ayuntamiento, codigoPostal)
                : new AcantiladoLocation(ayuntamiento, codigoPostal, barrio);
    }

    public int getDeduplicatedAyuntamientos() {
        return deduplicatedAyuntamientos.size();
    }

    public int getBruteForceCodigoPostalMatches() {
        return bruteForceCodigoPostalMatches.get();
    }

    public int getLocationsEstablished() {
        return locationsEstablished.get();
    }

    public AtomicInteger getBarriosEstablished() {
        return barriosEstablished;
    }

    public AtomicInteger getBarrioMisses() {
        return barrioMisses;
    }

    private CodigoPostal findCodigoPostal(Ayuntamiento ayuntamiento, Point point) {
        // Find hits based on establish geographic overlaps between ayuntamientos and postal codes. This should work.
        for (CodigoPostal codigoPostal : ayuntamiento.getCodigosPostales()) {
            if (codigoPostal.getGeometry().covers(point)) {
                return codigoPostal;
            }
        }

        // Fall back to brute force matching. This should only happen on unintentional search results; El Pinar
        // exists in Granada but also on one of the Spanish islands near Africa.
        double minDistance = Double.MAX_VALUE;
        CodigoPostal closest = null;
        for (CodigoPostal codigoPostal : codigoPostalDAO.findAll()) {
            double distance = point.distance(codigoPostal.getGeometry());
            if (distance < minDistance) {
                minDistance = distance;
                closest = codigoPostal;
            }
        }

        LOGGER.debug("Found codigo postal {} by proximity of ~{}m", closest.getCodigoPostal(), (int) (minDistance * 111000));
        bruteForceCodigoPostalMatches.incrementAndGet();
        return closest;
    }

    private Barrio findBarrio(Ayuntamiento ayuntamiento, Point point, long propertyCode) {
        Set<Long> codesOfCitiesWithBarrios = new HashSet<>();
        for (CityAyuntamientoCodes code : CityAyuntamientoCodes.values()) {
            codesOfCitiesWithBarrios.add(code.cityCode);
        }

        if (codesOfCitiesWithBarrios.contains(ayuntamiento.getId())) {
            for (Barrio barrio : barrioDAO.findByAyuntamiento(ayuntamiento.getId())) {
                if (barrio.getGeometry().contains(point)) {
                    LOGGER.debug("Successfully found barrio for ayuntamiento {} and {}", ayuntamiento.getId(), propertyCode);
                    barriosEstablished.incrementAndGet();
                    return barrio;
                }
            }
            // This happens when a listing is technically in the ayuntamiento but not covered by a city's barrio
            // geo features. This seems to be a small portion (~0.5%) so not as urgent to fix.
            barrioMisses.incrementAndGet();
            LOGGER.debug("Expected a barrio but none found for {} and {}", ayuntamiento.getId(), propertyCode   );
        }

        return null;
    }

    private Ayuntamiento findCorrectAyuntamiento(List<Ayuntamiento> ayuntamientos, Double latitude, Double longitude) {
        Point locationPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        Optional<Ayuntamiento> maybeClosestAyuntamiento = ayuntamientos.stream()
                .min(Comparator.comparingDouble(a -> a.getGeometry().distance(locationPoint)))
                .map(closest -> {
                    LOGGER.debug("Selecting ayuntamiento {} out of {}", closest, ayuntamientos);
                    return closest;
                });
        return maybeClosestAyuntamiento.orElseThrow();
    }
}
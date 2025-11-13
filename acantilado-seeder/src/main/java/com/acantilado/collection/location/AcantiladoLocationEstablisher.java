package com.acantilado.collection.location;

import com.acantilado.core.administrative.*;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AcantiladoLocationEstablisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcantiladoLocationEstablisher.class);

    private final ExistingLocationEstablisher existingLocationEstablisher;
    private final NovelLocationEstablisher novelLocationEstablisher;

    private final Set<Barrio> barriosForProvince;
    private final Set<Long> ayuntamientosWithBarrios;
    private final Map<Long, Set<CodigoPostal>> postCodesForProvince;

    private final AtomicBoolean isBootstrapMode = new AtomicBoolean(false);

    public AcantiladoLocationEstablisher(
            Set<Barrio> barriosForProvince,
            Set<Ayuntamiento> ayuntamientosForProvince,
            Map<Long, Set<CodigoPostal>> postCodesForProvince,
            AyuntamientoDAO ayuntamientoDAO,
            IdealistaLocationMappingDAO mappingDAO) {

        this.barriosForProvince = barriosForProvince;
        this.postCodesForProvince = postCodesForProvince;

        this.ayuntamientosWithBarrios = barriosForProvince
                .stream()
                .map(Barrio::getAyuntamientoId)
                .collect(Collectors.toSet());

        this.existingLocationEstablisher = new ExistingLocationEstablisher(
                ayuntamientoDAO,
                mappingDAO,
                this::buildAcantiladoLocation);

        this.novelLocationEstablisher = new NovelLocationEstablisher(
                mappingDAO,
                ayuntamientosForProvince,
                this::buildAcantiladoLocation);
    }

    public AcantiladoLocation establish(String idealistaAyuntamiento, String idealistaLocationId, Point locationPoint) {
        return isBootstrapMode.get()
                ? novelLocationEstablisher.establish(idealistaAyuntamiento, idealistaLocationId, locationPoint)
                : existingLocationEstablisher.establish(idealistaLocationId, locationPoint);
    }

    public void storeInMemoryMappings() {
        this.novelLocationEstablisher.storeInMemoryMappings();
    }

    public void setBootstrapMode(boolean shouldBootstrap) {
        this.isBootstrapMode.set(shouldBootstrap);
    }

    private AcantiladoLocation buildAcantiladoLocation(Ayuntamiento ayuntamiento, Point locationPoint) {
        CodigoPostal codigoPostal = findCodigoPostal(ayuntamiento, locationPoint);
        Optional<Barrio> maybeBarrio = findBarrio(ayuntamiento, locationPoint);

        return maybeBarrio
                .map(barrio -> new AcantiladoLocation(ayuntamiento, codigoPostal, barrio))
                .orElseGet(() -> new AcantiladoLocation(ayuntamiento, codigoPostal));
    }

    private CodigoPostal findCodigoPostal(Ayuntamiento ayuntamiento, Point locationPoint) {
        Set<CodigoPostal> postcodesForAyuntamiento = this.postCodesForProvince.get(ayuntamiento.getId());
        for (CodigoPostal codigoPostal : postcodesForAyuntamiento) {
            if (codigoPostal.getGeometry().covers(locationPoint)) {
                return codigoPostal;
            }
        }

        Optional<CodigoPostal> maybeClosestPostcode = postcodesForAyuntamiento.stream()
                .min(Comparator.comparingDouble(p -> p.getGeometry().distance(locationPoint)))
                .map(closest -> {
                    LOGGER.debug("Selecting closest postcode {} for location {}", closest.getCodigoPostal(), locationPoint);
                    return closest;
                });

        return maybeClosestPostcode.orElseThrow();
    }

    private Optional<Barrio> findBarrio(Ayuntamiento ayuntamiento, Point point) {
        if (ayuntamientosWithBarrios.contains(ayuntamiento.getId())) {
            for (Barrio barrio : barriosForProvince) {
                if (barrio.getGeometry().contains(point)) {
                    return Optional.of(barrio);
                }
            }
        }

        return Optional.empty();
    }
}
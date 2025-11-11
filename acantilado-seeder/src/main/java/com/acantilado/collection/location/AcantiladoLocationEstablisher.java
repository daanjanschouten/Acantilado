package com.acantilado.collection.location;

import com.acantilado.core.administrative.*;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AcantiladoLocationEstablisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcantiladoLocationEstablisher.class);
    public record CollectionStats(
            int locationsEstablished,
            int locationsMissed,
            int barriosEstablished,
            int barriosMissed) {}

    private final ExistingLocationEstablisher existingLocationEstablisher;
    private final NovelLocationEstablisher novelLocationEstablisher;

    private final AtomicInteger locationsEstablished = new AtomicInteger();
    private final AtomicInteger locationsMissed = new AtomicInteger();
    private final AtomicInteger barriosEstablished = new AtomicInteger();
    private final AtomicInteger barrioMisses = new AtomicInteger();

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

    public Optional<AcantiladoLocation> establish(String idealistaAyuntamiento, String idealistaLocationId, Point locationPoint) {
        Optional<AcantiladoLocation> maybeLocation;
        if (isBootstrapMode.get()) {
            maybeLocation = novelLocationEstablisher.establish(idealistaAyuntamiento, idealistaLocationId, locationPoint);
        } else {
            maybeLocation = existingLocationEstablisher.establish(idealistaLocationId, locationPoint);
        }

        if (maybeLocation.isEmpty()) {
            this.locationsMissed.incrementAndGet();
        } else {
            this.locationsEstablished.incrementAndGet();
        }
        return maybeLocation;
    }

    public void storeInMemoryMappings() {
        this.novelLocationEstablisher.storeInMemoryMappings();
    }

    public void setBootstrapMode(boolean shouldBootstrap) {
        this.isBootstrapMode.set(shouldBootstrap);
    }

    private Optional<AcantiladoLocation> buildAcantiladoLocation(Ayuntamiento ayuntamiento, Point locationPoint) {
        Optional<CodigoPostal> maybeCodigoPostal = findCodigoPostal(ayuntamiento, locationPoint);
        Optional<Barrio> maybeBarrio = findBarrio(ayuntamiento, locationPoint);

        if (maybeCodigoPostal.isEmpty()) {
            LOGGER.error("Unable to establish postcode for point {} in ayuntamiento {}", locationPoint, ayuntamiento);
            return Optional.empty();
        }

        AcantiladoLocation location = maybeBarrio
                .map(barrio -> new AcantiladoLocation(ayuntamiento, maybeCodigoPostal.get(), barrio))
                .orElseGet(() -> new AcantiladoLocation(ayuntamiento, maybeCodigoPostal.get()));

        return Optional.of(location);
    }

    public CollectionStats getCollectionStats() {
        return new CollectionStats(
                locationsEstablished.get(),
                locationsMissed.get(),
                barriosEstablished.get(),
                barrioMisses.get()
        );
    }

    private Optional<CodigoPostal> findCodigoPostal(Ayuntamiento ayuntamiento, Point point) {
        Set<CodigoPostal> postcodesForAyuntamiento = this.postCodesForProvince.get(ayuntamiento.getId());
        for (CodigoPostal codigoPostal : postcodesForAyuntamiento) {
            if (codigoPostal.getGeometry().covers(point)) {
                return Optional.of(codigoPostal);
            }
        }
        return Optional.empty();
    }

    private Optional<Barrio> findBarrio(Ayuntamiento ayuntamiento, Point point) {
        if (ayuntamientosWithBarrios.contains(ayuntamiento.getId())) {
            for (Barrio barrio : barriosForProvince) {
                if (barrio.getGeometry().contains(point)) {
                    barriosEstablished.incrementAndGet();
                    return Optional.of(barrio);
                }
            }
            barrioMisses.incrementAndGet();
        }

        return Optional.empty();
    }
}
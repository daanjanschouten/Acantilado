package com.acantilado.collection.location;

import com.acantilado.core.administrative.*;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AcantiladoLocationEstablisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(AcantiladoLocationEstablisher.class);

  private final ExistingLocationEstablisher existingLocationEstablisher;

  private final Set<Ayuntamiento> ayuntamientosForProvince;
  private final Set<Barrio> barriosForProvince;
  private final Set<String> ayuntamientosWithBarrios;
  private final Map<String, Set<CodigoPostal>> postCodesForProvince;

  private final IdealistaLocationMappingDAO mappingDAO;

  private final Map<IdealistaLocationMapping, Integer> mappingsByCounts = new HashMap<>();

  private final AtomicBoolean isBootstrapMode = new AtomicBoolean(false);

  public AcantiladoLocationEstablisher(
      Set<Barrio> barriosForProvince,
      Set<Ayuntamiento> ayuntamientosForProvince,
      Map<String, Set<CodigoPostal>> postCodesForProvince,
      AyuntamientoDAO ayuntamientoDAO,
      IdealistaLocationMappingDAO mappingDAO) {

    if (barriosForProvince.isEmpty()) {
      LOGGER.info("Found no barrios for province");
    }

    this.ayuntamientosForProvince = ayuntamientosForProvince;
    this.barriosForProvince = barriosForProvince;
    this.postCodesForProvince = postCodesForProvince;
    this.mappingDAO = mappingDAO;

    this.ayuntamientosWithBarrios =
        barriosForProvince.stream().map(Barrio::getAyuntamientoId).collect(Collectors.toSet());

    this.existingLocationEstablisher =
        new ExistingLocationEstablisher(ayuntamientoDAO, mappingDAO, this::buildAcantiladoLocation);
  }

  public AcantiladoLocation establishForLocation(Point locationPoint) {
    Ayuntamiento ayuntamiento = establishAyuntamientoByCoordinates(locationPoint);
    return buildAcantiladoLocation(ayuntamiento, locationPoint);
  }

  public AcantiladoLocation establish(
      String idealistaAyuntamiento, String idealistaLocationId, Point locationPoint) {
    return isBootstrapMode.get()
        ? establishForIdealista(idealistaAyuntamiento, idealistaLocationId, locationPoint)
        : existingLocationEstablisher.establish(idealistaLocationId, locationPoint);
  }

  private AcantiladoLocation establishForIdealista(
      String idealistaAyuntamiento, String locationId, Point locationPoint) {
    Ayuntamiento ayuntamiento = establishAyuntamientoByCoordinates(locationPoint);
    IdealistaLocationMapping mapping =
        new IdealistaLocationMapping(
            locationId, idealistaAyuntamiento, ayuntamiento.getId(), ayuntamiento.getName());

    updateInMemoryMapping(mapping);
    return buildAcantiladoLocation(ayuntamiento, locationPoint);
  }

  public void storeMapping(IdealistaLocationMapping mapping) {
    this.mappingDAO.merge(mapping);
  }

  public void storeInMemoryMappings() {
    AtomicInteger mappingsStored = new AtomicInteger();
    mappingsByCounts.forEach(
        (mapping, count) -> {
          List<IdealistaLocationMapping> existingMappings =
              mappingDAO.findByAyuntamientoId(mapping.getAcantiladoAyuntamientoId());
          if (existingMappings.contains(mapping)) {
            LOGGER.debug("Mapping {} already existed, skipping", mapping);
            return;
          }

          LOGGER.info("Created new mapping: {} with score {}", mapping, count);
          mappingsStored.incrementAndGet();
          mappingDAO.merge(mapping);
        });

    LOGGER.info("Finished storing {} mappings", mappingsStored.get());
  }

  public void setBootstrapMode(boolean shouldBootstrap) {
    this.isBootstrapMode.set(shouldBootstrap);
  }

  private AcantiladoLocation buildAcantiladoLocation(
      Ayuntamiento ayuntamiento, Point locationPoint) {
    CodigoPostal codigoPostal = findCodigoPostal(ayuntamiento, locationPoint);
    Optional<Barrio> maybeBarrio = findBarrio(ayuntamiento, locationPoint);

    return maybeBarrio
        .map(barrio -> new AcantiladoLocation(ayuntamiento, codigoPostal, barrio))
        .orElseGet(() -> new AcantiladoLocation(ayuntamiento, codigoPostal));
  }

  private void updateInMemoryMapping(IdealistaLocationMapping mapping) {
    if (mappingsByCounts.containsKey(mapping)) {
      mappingsByCounts.compute(mapping, (k, existingCount) -> existingCount + 1);
    } else {
      mappingsByCounts.put(mapping, 1);
    }
  }

  private Ayuntamiento establishAyuntamientoByCoordinates(Point locationPoint) {
    for (Ayuntamiento ayuntamiento : ayuntamientosForProvince) {
      if (ayuntamiento.getGeometry().contains(locationPoint)) {
        LOGGER.debug("Point is inside ayuntamiento {}", ayuntamiento.getName());
        return ayuntamiento;
      }
    }

    Optional<Ayuntamiento> maybeClosestAyuntamiento =
        ayuntamientosForProvince.stream()
            .min(Comparator.comparingDouble(a -> a.getGeometry().distance(locationPoint)))
            .map(
                closest -> {
                  LOGGER.debug(
                      "Selecting closest ayuntamiento {} for location {}",
                      closest.getName(),
                      locationPoint);
                  return closest;
                });

    return maybeClosestAyuntamiento.orElseThrow();
  }

  private CodigoPostal findCodigoPostal(Ayuntamiento ayuntamiento, Point locationPoint) {
    Set<CodigoPostal> postcodesForAyuntamiento =
        this.postCodesForProvince.get(ayuntamiento.getId());

    if (postcodesForAyuntamiento == null || postcodesForAyuntamiento.isEmpty()) {
      LOGGER.warn(
          "No postcodes found for ayuntamiento {} - location may be outside province boundaries",
          ayuntamiento.getId());
      throw new IllegalStateException(
          "No postcodes available for ayuntamiento "
              + ayuntamiento.getName()
              + " ("
              + ayuntamiento.getId()
              + ")");
    }

    for (CodigoPostal codigoPostal : postcodesForAyuntamiento) {
      if (codigoPostal.getGeometry().covers(locationPoint)) {
        return codigoPostal;
      }
    }

    Optional<CodigoPostal> maybeClosestPostcode =
        postcodesForAyuntamiento.stream()
            .min(Comparator.comparingDouble(p -> p.getGeometry().distance(locationPoint)))
            .map(
                closest -> {
                  LOGGER.debug(
                      "Selecting closest postcode {} for location {}",
                      closest.getCodigoPostal(),
                      locationPoint);
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

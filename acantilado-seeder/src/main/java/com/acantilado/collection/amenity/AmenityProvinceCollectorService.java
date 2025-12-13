package com.acantilado.collection.amenity;

import com.acantilado.collection.apify.ApifySearchResults;
import com.acantilado.collection.location.AcantiladoLocationEstablisher;
import com.acantilado.core.administrative.*;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import com.acantilado.core.amenity.fields.GoogleAmenityCategory;
import com.acantilado.utils.ProvinceCollectionUtils;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmenityProvinceCollectorService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AmenityProvinceCollectorService.class);

  private final SessionFactory sessionFactory;
  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  private final CodigoPostalDAO codigoPostalDAO;

  private final Provincia provinceToCollectFor;

  private final GoogleAmenityCollector amenityCollector;
  private final Set<String> postcodeIdsForProvince;
  private final Set<String> ayuntamientoIdsForProvince;

  public AmenityProvinceCollectorService(
      String provinceId,
      GoogleAmenityDAO amenityDAO,
      GoogleAmenitySnapshotDAO snapshotDAO,
      SessionFactory sessionFactory,
      ProvinciaDAO provinciaDAO,
      CodigoPostalDAO codigoPostalDAO,
      AyuntamientoDAO ayuntamientoDAO,
      BarrioDAO barrioDAO,
      IdealistaLocationMappingDAO mappingDAO) {
    this.sessionFactory = sessionFactory;
    this.provinceToCollectFor =
        ProvinceCollectionUtils.getProvinceFromId(sessionFactory, provinciaDAO, provinceId);
    this.postcodeIdsForProvince =
        ProvinceCollectionUtils.getPostcodeIdsForProvince(
            sessionFactory, ayuntamientoDAO, provinceId);

    this.codigoPostalDAO = codigoPostalDAO;

    Set<Ayuntamiento> ayuntamientosForProvince =
        ProvinceCollectionUtils.getAyuntamientosForProvince(
            sessionFactory, ayuntamientoDAO, provinceToCollectFor);
    AcantiladoLocationEstablisher locationEstablisher =
        new AcantiladoLocationEstablisher(
            ProvinceCollectionUtils.getBarriosForProvince(
                sessionFactory, barrioDAO, provinceToCollectFor),
            ayuntamientosForProvince,
            ProvinceCollectionUtils.getPostcodesForAyuntamientos(
                sessionFactory, codigoPostalDAO, ayuntamientosForProvince),
            ayuntamientoDAO,
            mappingDAO);

    this.ayuntamientoIdsForProvince =
        ayuntamientosForProvince.stream().map(Ayuntamiento::getId).collect(Collectors.toSet());
    this.amenityCollector =
        new GoogleAmenityCollector(
            amenityDAO, snapshotDAO, executorService, sessionFactory, locationEstablisher);
  }

  public void shutdownExecutor() {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
    }
  }

  public boolean collectAmenitiesForProvince(GoogleAmenityCategory category) {
    Set<CodigoPostal> postcodesForProvince =
        ProvinceCollectionUtils.getPostcodesForAyuntamientoIds(
            sessionFactory, codigoPostalDAO, ayuntamientoIdsForProvince);

    Set<GoogleAmenitySearchRequest> searchRequests;
    if (category.getCriticality() == GoogleAmenityCategory.Criticality.URBAN) {
      LOGGER.info("Collecting amenities at the province level for {}", category);
      searchRequests =
          Set.of(new GoogleAmenitySearchRequest(provinceToCollectFor.getGeometryJson(), category));
    } else {
      LOGGER.info("Collecting amenities at the postcode level for {}", category);
      searchRequests =
          postcodesForProvince.stream()
              .map(
                  cp ->
                      new GoogleAmenitySearchRequest(
                          cp.getGeometryJson(), GoogleAmenityCategory.SUPERMARKET))
              .limit(3)
              .collect(Collectors.toSet());
    }

    ApifySearchResults<GoogleAmenitySearchRequest> results =
        amenityCollector.startCollection(searchRequests);

    if (results.requestsSucceeded().size() == searchRequests.size()) {
      LOGGER.info("Finished amenity collection for province {}", provinceToCollectFor.getName());
    } else {
      LOGGER.warn("Amenity collection for province finished partially: {} ", results);
    }

    return true;
  }
}

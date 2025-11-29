package com.acantilado.collection.amenity;

import com.acantilado.collection.apify.ApifySearchResults;
import com.acantilado.core.administrative.*;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.utils.ProvinceCollectionUtils;
import com.acantilado.utils.RetryableBatchedExecutor;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AmenityProvinceCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmenityProvinceCollectorService.class);
    private static final Optional<Integer> APIFY_ACTIVE_AGENTS = Optional.of(32);

    private final SessionFactory sessionFactory;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // location establisher

    private final GoogleAmenityCollector amenityCollector;
    private final Provincia provinceToCollectFor;
    private final Set<Ayuntamiento> ayuntamientosForProvince;

    public AmenityProvinceCollectorService(
            String provinceName,
            GoogleAmenityDAO amenityDAO,
            GoogleAmenitySnapshotDAO snapshotDAO,
            SessionFactory sessionFactory,
            ProvinciaDAO provinciaDAO,
            AyuntamientoDAO ayuntamientoDAO) {
        this.sessionFactory = sessionFactory;

        this.provinceToCollectFor = ProvinceCollectionUtils.getProvinceFromName(
                sessionFactory, provinciaDAO, provinceName);
        this.ayuntamientosForProvince = ProvinceCollectionUtils.getAyuntamientosForProvince(
                sessionFactory, ayuntamientoDAO, provinceToCollectFor);

        this.amenityCollector = new GoogleAmenityCollector(amenityDAO, snapshotDAO, executorService, sessionFactory);
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

    public boolean collectAmenitiesForProvince() {
        LOGGER.info("Starting full amenity collection workflow for province {}", provinceToCollectFor.getName());

        Set<GoogleAmenitySearchRequest> searchRequests =
                RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction(sessionFactory,
                        () -> ayuntamientosForProvince.stream()
                                .flatMap(ayuntamiento ->
                                        ayuntamiento.getCodigosPostales().stream()
                                                .map(CodigoPostal::getCodigoPostal)
                                                .map(postcode -> new GoogleAmenitySearchRequest(
                                                        ayuntamiento.getName(),
                                                        postcode,
                                                        AcantiladoAmenityChain.CARREFOUR)))
                                .collect(Collectors.toSet()));

        ApifySearchResults<GoogleAmenitySearchRequest> results = amenityCollector.startCollection(searchRequests);

        if (results.requestsSucceeded().size() == searchRequests.size()) {
            LOGGER.info("Finished amenity collection for province {}", provinceToCollectFor.getName());
        }
        LOGGER.warn("Amenity collection for province finished with failures {}", results);

        return true;
    }
}

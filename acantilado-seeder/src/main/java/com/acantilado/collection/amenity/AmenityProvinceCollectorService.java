package com.acantilado.collection.amenity;

import com.acantilado.collection.apify.ApifySearchResults;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.ProvinciaDAO;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.utils.ProvinceCollectionUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AmenityProvinceCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmenityProvinceCollectorService.class);

    private final SessionFactory sessionFactory;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final AyuntamientoDAO ayuntamientoDAO;
    private final String provinceName;

    // location establisher

    private final GoogleAmenityCollector amenityCollector;
    private final Map<String, Set<String>> postCodeIdsByAyuntamientoName;

    public AmenityProvinceCollectorService(
            String provinceName,
            GoogleAmenityDAO amenityDAO,
            GoogleAmenitySnapshotDAO snapshotDAO,
            SessionFactory sessionFactory,
            ProvinciaDAO provinciaDAO,
            AyuntamientoDAO ayuntamientoDAO) {
        this.provinceName = provinceName;
        this.sessionFactory = sessionFactory;
        this.ayuntamientoDAO = ayuntamientoDAO;

        this.postCodeIdsByAyuntamientoName = ProvinceCollectionUtils.getPostcodeIdsForProvince(
                sessionFactory, provinciaDAO, ayuntamientoDAO, provinceName);

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
        LOGGER.info("Starting full amenity collection workflow for province {}", provinceName);

        Set<GoogleAmenitySearchRequest> searchRequests = postCodeIdsByAyuntamientoName
                .entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                        .stream()
                        .map(postcode -> new GoogleAmenitySearchRequest(
                                entry.getKey(),
                                postcode,
                                AcantiladoAmenityChain.CARREFOUR)))
                .collect(Collectors.toSet());

        ApifySearchResults<GoogleAmenitySearchRequest> results = amenityCollector.startCollection(searchRequests);

        if (results.requestsSucceeded().size() == searchRequests.size()) {
            LOGGER.info("Finished amenity collection for province {}", provinceName);
        }
        LOGGER.warn("Amenity collection for province finished partially {} ", results);

        return true;
    }
}

package com.acantilado.collection.amenity;

import com.acantilado.collection.apify.ApifySearchResults;
import com.acantilado.collection.location.AcantiladoLocationEstablisher;
import com.acantilado.core.administrative.*;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.utils.ProvinceCollectionUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AmenityProvinceCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmenityProvinceCollectorService.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final Provincia provinceToCollectFor;

    private final GoogleAmenityCollector amenityCollector;
    private final Set<String> postcodeIdsForProvince;

    public AmenityProvinceCollectorService(
            String provinceName,
            GoogleAmenityDAO amenityDAO,
            GoogleAmenitySnapshotDAO snapshotDAO,
            SessionFactory sessionFactory,
            ProvinciaDAO provinciaDAO,
            CodigoPostalDAO codigoPostalDAO,
            AyuntamientoDAO ayuntamientoDAO,
            BarrioDAO barrioDAO,
            IdealistaLocationMappingDAO mappingDAO) {
        this.provinceToCollectFor = ProvinceCollectionUtils.getProvinceFromName(
                sessionFactory, provinciaDAO, provinceName);
        this.postcodeIdsForProvince = ProvinceCollectionUtils.getPostcodeIdsForProvince(
                sessionFactory, provinciaDAO, ayuntamientoDAO, provinceName);

        Set<Ayuntamiento> ayuntamientosForProvince = ProvinceCollectionUtils.getAyuntamientosForProvince(
                sessionFactory, ayuntamientoDAO, provinceToCollectFor);
        AcantiladoLocationEstablisher locationEstablisher = new AcantiladoLocationEstablisher(
                ProvinceCollectionUtils.getBarriosForProvince(sessionFactory, barrioDAO, provinceToCollectFor),
                ayuntamientosForProvince,
                ProvinceCollectionUtils.getPostcodesForAyuntamientos(
                        sessionFactory, codigoPostalDAO, ayuntamientosForProvince),
                ayuntamientoDAO,
                mappingDAO);

        this.amenityCollector = new GoogleAmenityCollector(
                amenityDAO,
                snapshotDAO,
                executorService,
                sessionFactory,
                locationEstablisher);
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

        Set<GoogleAmenitySearchRequest> searchRequests = postcodeIdsForProvince
                .stream()
                .map(postcode ->
                    new GoogleAmenitySearchRequest(postcode, AcantiladoAmenityChain.CARREFOUR)
                )
                .collect(Collectors.toSet());

        ApifySearchResults<GoogleAmenitySearchRequest> results = amenityCollector.startCollection(searchRequests);

        if (results.requestsSucceeded().size() == searchRequests.size()) {
            LOGGER.info("Finished amenity collection for province {}", provinceToCollectFor.getName());
        } else {
            LOGGER.warn("Amenity collection for province finished partially: {} ", results);
        }

        return true;
    }
}

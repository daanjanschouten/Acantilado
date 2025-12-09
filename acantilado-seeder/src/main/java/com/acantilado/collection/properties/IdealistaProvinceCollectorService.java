package com.acantilado.collection.properties;

import com.acantilado.collection.apify.ApifyCollector;
import com.acantilado.collection.apify.ApifySearchResults;
import com.acantilado.collection.location.AcantiladoLocation;
import com.acantilado.collection.location.AcantiladoLocationEstablisher;
import com.acantilado.collection.properties.collectors.IdealistaLocationCollector;
import com.acantilado.collection.properties.collectors.IdealistaRealEstateCollector;
import com.acantilado.collection.properties.idealista.IdealistaPropertyType;
import com.acantilado.collection.properties.idealista.IdealistaSearchRequest;
import com.acantilado.core.administrative.*;
import com.acantilado.core.idealista.IdealistaContactInformationDAO;
import com.acantilado.core.idealista.IdealistaLocationDAO;
import com.acantilado.core.idealista.IdealistaPropertyDAO;
import com.acantilado.core.idealista.IdealistaTerrainDAO;
import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import com.acantilado.export.LocationMappingMerchant;
import com.acantilado.utils.ProvinceCollectionUtils;
import com.acantilado.utils.RetryableBatchedExecutor;
import com.google.common.collect.Sets;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.acantilado.utils.RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction;

public final class IdealistaProvinceCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaProvinceCollectorService.class);

    private final IdealistaLocationMappingDAO mappingDAO;
    private final IdealistaLocationDAO locationDAO;

    private final AyuntamientoDAO ayuntamientoDAO;

    private final SessionFactory sessionFactory;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final AcantiladoLocationEstablisher locationEstablisher;

    private final IdealistaRealEstateCollector<IdealistaTerrain> terrainCollector;
    private final IdealistaRealEstateCollector<IdealistaProperty> propertyCollector;
    private final IdealistaLocationCollector locationCollector;
    private final LocationMappingMerchant locationMappingMerchant;

    private final Provincia provinceToCollectFor;
    private final Set<Ayuntamiento> ayuntamientosForProvince;

    public IdealistaProvinceCollectorService(
            String provinceName,
            IdealistaContactInformationDAO contactDAO,
            IdealistaPropertyDAO propertyDAO,
            IdealistaTerrainDAO terrainDAO,
            IdealistaLocationDAO locationDAO,
            ProvinciaDAO provinciaDAO,
            CodigoPostalDAO codigoPostalDAO,
            AyuntamientoDAO ayuntamientoDAO,
            BarrioDAO barrioDAO,
            IdealistaLocationMappingDAO mappingDAO,
            SessionFactory sessionFactory) {
        this.mappingDAO = mappingDAO;
        this.locationDAO = locationDAO;
        this.ayuntamientoDAO = ayuntamientoDAO;

        this.sessionFactory = sessionFactory;

        this.provinceToCollectFor = ProvinceCollectionUtils.getProvinceFromName(
                sessionFactory, provinciaDAO, provinceName);
        this.ayuntamientosForProvince = ProvinceCollectionUtils.getAyuntamientosForProvince(
                sessionFactory, ayuntamientoDAO, provinceToCollectFor);

        locationEstablisher = new AcantiladoLocationEstablisher(
                ProvinceCollectionUtils.getBarriosForProvince(sessionFactory, barrioDAO, provinceToCollectFor),
                ayuntamientosForProvince,
                ProvinceCollectionUtils.getPostcodesForAyuntamientos(
                        sessionFactory, codigoPostalDAO, ayuntamientosForProvince),
                ayuntamientoDAO,
                mappingDAO);

        terrainCollector = new IdealistaRealEstateCollector<>(
                locationEstablisher,
                contactDAO,
                terrainDAO,
                IdealistaTerrain::constructFromJson,
                executorService,
                sessionFactory);

        propertyCollector = new IdealistaRealEstateCollector<>(
                locationEstablisher,
                contactDAO,
                propertyDAO,
                IdealistaProperty::constructFromJson,
                executorService,
                sessionFactory);

        locationCollector = new IdealistaLocationCollector(locationDAO, executorService, sessionFactory);
        locationMappingMerchant = new LocationMappingMerchant(mappingDAO, sessionFactory);
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

    /**
     * Phase 1: Ensure all Idealista location IDs are collected for the province
     * @return true if location IDs are complete, false if manual intervention needed
     */
    private boolean ensureLocationIdsComplete() {
        Set<IdealistaAyuntamientoLocation> locations = ProvinceCollectionUtils.getLocationsForProvince(
                sessionFactory, locationDAO, provinceToCollectFor);
        int locationCount = locations.size();
        int ayuntamientoCount = ayuntamientosForProvince.size();

        if (locationCount >= ayuntamientoCount) {
            LOGGER.info("Location IDs complete: {} location IDs for {} ayuntamientos",
                    locationCount, ayuntamientoCount);
            return true;
        }
        LOGGER.info("Insufficient location IDs {} for {} ayuntamientos; bootstrapping location IDs",
                locationCount, ayuntamientoCount);

        IdealistaSearchRequest homeRequest = IdealistaSearchRequest.locationBasedSaleSearch(
                provinceToCollectFor.getIdealistaLocationId(),
                IdealistaPropertyType.HOMES);
        IdealistaSearchRequest landRequest = IdealistaSearchRequest.locationBasedSaleSearch(
                provinceToCollectFor.getIdealistaLocationId(),
                IdealistaPropertyType.LANDS);

        startRealEstateCollectionForProvince(
                Set.of(homeRequest, landRequest),
                locationCollector);

        locations = ProvinceCollectionUtils.getLocationsForProvince(
                sessionFactory, locationDAO, provinceToCollectFor);
        if (locations.size() < ayuntamientoCount) {
            LOGGER.error("Still insufficient location IDs {} for {} ayuntamientos. Manual intervention required.",
                    locations.size(), ayuntamientoCount);
            return false;
        }

        LOGGER.info("Successfully completed Phase 1: {} location IDs collected", locations.size());
        return true;
    }

    /**
     * Phase 2: Build mappings from Idealista location IDs to Acantilado ayuntamientos
     * @return true if all mappings are complete, false if some couldn't be mapped
     */
    private boolean bootstrapLocationMappings() {
        Set<String> missingLocations = getLocationIdsMissingFromMappings();

        if (missingLocations.isEmpty()) {
            LOGGER.info("No missing location IDs to bootstrap, moving on");
            return true;
        }

        LOGGER.info("Building missing mappings for {} location IDs", missingLocations.size());
        locationEstablisher.setBootstrapMode(true);

        Set<IdealistaSearchRequest> homeRequests = missingLocations.stream()
                .map(IdealistaSearchRequest::homeSaleSearch)
                .collect(Collectors.toSet());
        Set<IdealistaSearchRequest> landRequests = missingLocations.stream()
                .map(IdealistaSearchRequest::landSaleSearch)
                .collect(Collectors.toSet());

        startRealEstateCollectionForProvince(Sets.union(homeRequests, landRequests), propertyCollector);

        RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(
                sessionFactory,
                locationEstablisher::storeInMemoryMappings);

        locationEstablisher.setBootstrapMode(false);

        Set<String> missing = getLocationIdsMissingFromMappings();
        LOGGER.info("Proceeding to extract ayuntamiento codes from {} remaining location IDs", missing.size());

        RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(sessionFactory, () ->
                missing.forEach(missingLocationId -> {
                    String ayuntamientoId = AcantiladoLocation.getAyuntamientoFromNormalizedLocationId(missingLocationId);
                    Optional<Ayuntamiento> maybeAyuntamiento = ayuntamientoDAO.findById(ayuntamientoId);

                    if (maybeAyuntamiento.isEmpty()) {
                        return;
                    }

                    Ayuntamiento ayuntamiento = maybeAyuntamiento.get();
                    IdealistaLocationMapping mapping = new IdealistaLocationMapping(
                            missingLocationId,
                            ayuntamiento.getName(),
                            ayuntamiento.getId(),
                            ayuntamiento.getName());
                    LOGGER.info("Established mapping {} from a missing location ID", mapping);
                    locationEstablisher.storeMapping(mapping);
                }));


        Set<String> stillMissing = getLocationIdsMissingFromMappings();
        if (!stillMissing.isEmpty()) {
            LOGGER.warn("Could not create mappings for {} location IDs: {}",
                    stillMissing.size(), stillMissing);
            return false;
        }

        LOGGER.info("Successfully completed Phase 2: All mappings for locations created");
        return true;
    }

    /**
     * Phase 3: Build mappings from Idealista location IDs to Acantilado ayuntamientos
     * @return true if all mappings are complete, false if some couldn't be mapped
     */
    private boolean bootstrapAyuntamientoMappings() {
        Set<String> missingAyuntamientoIds = getAyuntamientoIdsMissingFromMappings();
        if (missingAyuntamientoIds.isEmpty()) {
            LOGGER.info("No missing ayuntamientos to bootstrap, moving on");
            return true;
        }

        LOGGER.info("Building missing mappings for {} ayuntamientos", missingAyuntamientoIds.size());
        locationEstablisher.setBootstrapMode(true);

        Set<String> missingAyuntamientos = ayuntamientosForProvince
                .stream()
                .filter(a -> missingAyuntamientoIds.contains(a.getId()))
                .map(Ayuntamiento::getName)
                .collect(Collectors.toSet());

        Set<IdealistaSearchRequest> homeRequests = missingAyuntamientos.stream()
                .map(IdealistaSearchRequest::homeSaleSearch)
                .collect(Collectors.toSet());
        Set<IdealistaSearchRequest> landRequests = missingAyuntamientos.stream()
                .map(IdealistaSearchRequest::landSaleSearch)
                .collect(Collectors.toSet());

        startRealEstateCollectionForProvince(Sets.union(homeRequests, landRequests), propertyCollector);

        RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(
                sessionFactory,
                locationEstablisher::storeInMemoryMappings);

        locationEstablisher.setBootstrapMode(false);

        Set<String> stillMissing = getAyuntamientoIdsMissingFromMappings();
        if (!stillMissing.isEmpty()) {
            LOGGER.warn("Could not create mappings for {} ayuntamiento IDs: {}",
                    stillMissing.size(), stillMissing);
            return false;
        }

        LOGGER.info("Successfully completed Phase 3: All mappings for ayuntamientos created");
        return true;
    }

    /**
     * Phase 3: Collect real estate listings using the complete mappings
     */
    private boolean collectRealEstateListings(IdealistaPropertyType propertyType) {
        Set<IdealistaSearchRequest> searchRequests =
                executeCallableInSessionWithoutTransaction(sessionFactory, mappingDAO::findAll)
                        .stream()
                        .map(IdealistaLocationMapping::getIdealistaLocationId)
                        .filter(location -> location.contains(provinceToCollectFor.getIdealistaLocationId()))
                        .map(location -> IdealistaSearchRequest.saleSearch(location, propertyType))
                        .collect(Collectors.toSet());

        IdealistaRealEstateCollector<?> collector = switch (propertyType) {
            case HOMES -> propertyCollector;
            case LANDS -> terrainCollector;
        };

        return startRealEstateCollectionForProvince(searchRequests, collector);
    }

    public boolean collectRealEstateForProvince(IdealistaPropertyType propertyType) {
        LOGGER.info("Starting full collection workflow for province {} and property type {}",
                provinceToCollectFor.getName(), propertyType);

        Set<String> missingLocationMappings = getLocationIdsMissingFromMappings();
        Set<String> missingAyuntamientoMappings = getAyuntamientoIdsMissingFromMappings();
        boolean mappingsComplete = missingLocationMappings.isEmpty() && missingAyuntamientoMappings.isEmpty();

        if (mappingsComplete) {
            LOGGER.info("All mappings complete, skipping import and bootstrap phases");
            return collectRealEstateListings(propertyType);
        } else if (locationMappingMerchant.importMappings(provinceToCollectFor)) {
            LOGGER.info("Imported mappings from disk, starting collection");
            return collectRealEstateListings(propertyType);
        }

        LOGGER.info("Mappings incomplete: {} location IDs and {} ayuntamientos need mapping",
                missingLocationMappings.size(), missingAyuntamientoMappings.size());
        LOGGER.info("Missing ayuntamiento IDs: {}", missingAyuntamientoMappings);

        // Phase 1: Location IDs
        if (!ensureLocationIdsComplete()) {
            LOGGER.error("Phase 1 failed: Could not collect all location IDs. Manual intervention required.");
            return false;
        }

        // Phase 2: Bootstrap location mappings
        if (!bootstrapLocationMappings()) {
            LOGGER.error("Phase 2 failed: Could not create mappings for all locations. Manual intervention required.");
            return false;
        }

        // Phase 3: Bootstrap ayuntamiento mappings
        if (!bootstrapAyuntamientoMappings()) {
            LOGGER.error("Phase 3 failed: Could not create mappings for all ayuntamientos. Manual intervention required.");
            return false;
        }

        // Phase 4: Export mappings since they were generated
        LOGGER.info("Exporting mappings to disk since they were generated on the fly.");
        locationMappingMerchant.exportMappings(provinceToCollectFor);

        LOGGER.info("Finished initial bootstrap & collection cycle");
        return true;
    }

    /**
     * Get the set of location IDs that don't have mappings yet
     */
    private Set<String> getLocationIdsMissingFromMappings() {
        Set<IdealistaAyuntamientoLocation> locations = ProvinceCollectionUtils.getLocationsForProvince(
                sessionFactory, locationDAO, provinceToCollectFor);

        Set<String> allLocationIds = locations.stream()
                .map(IdealistaAyuntamientoLocation::getAyuntamientoLocationId)
                .collect(Collectors.toSet());

        Set<String> mappedLocationIds = ProvinceCollectionUtils
                .getMappingsByLocationIds(sessionFactory, mappingDAO, allLocationIds)
                .keySet();

        Set<String> missingIds = new HashSet<>(allLocationIds);
        missingIds.removeAll(mappedLocationIds);

        return missingIds;
    }

    /**
     * Get the set of ayuntamiento IDs that don't have mappings yet
     */
    private Set<String> getAyuntamientoIdsMissingFromMappings() {
        Set<String> allAyuntamientoIds = ayuntamientosForProvince
                .stream()
                .map(Ayuntamiento::getId)
                .collect(Collectors.toSet());

        // Get ALL mappings for this province's ayuntamientos
        // Don't filter by location IDs - we want to find mappings created by name-based searches too
        Set<String> mappedAyuntamientoIds = ayuntamientosForProvince
                .stream()
                .map(Ayuntamiento::getId)
                .filter(ayuntamientoId -> {
                    List<IdealistaLocationMapping> mappings = executeCallableInSessionWithoutTransaction(
                            sessionFactory,
                            () -> mappingDAO.findByAyuntamientoId(ayuntamientoId)
                    );
                    return !mappings.isEmpty();
                })
                .collect(Collectors.toSet());

        Set<String> missingIds = new HashSet<>(allAyuntamientoIds);
        missingIds.removeAll(mappedAyuntamientoIds);

        return missingIds;
    }

    private <T> boolean startRealEstateCollectionForProvince(
            Set<IdealistaSearchRequest> searchRequests, ApifyCollector<IdealistaSearchRequest, T> collector) {
        return startRealEstateCollectionForProvince(searchRequests, collector, 0);
    }

    private <T> boolean startRealEstateCollectionForProvince(
            Set<IdealistaSearchRequest> searchRequests,
            ApifyCollector<IdealistaSearchRequest, T> collector,
            int retryCount) {
        if (retryCount > 3) {
            LOGGER.info("Giving up on {} remaining requests after 3 retries", searchRequests.size());
            return true;
        }

        LOGGER.info("Orchestrating collection for retry {}", retryCount);
        ApifySearchResults<IdealistaSearchRequest> results = collector.startCollection(searchRequests);

        Set<IdealistaSearchRequest> remainingRequestsToRun = calculateRequestsToRetry(results);
        if (!remainingRequestsToRun.isEmpty()) {
            LOGGER.info("Processing {} remaining requests", remainingRequestsToRun.size());
            return startRealEstateCollectionForProvince(remainingRequestsToRun, collector, retryCount + 1);
        }

        return true;
    }

    private static Set<IdealistaSearchRequest> calculateRequestsToRetry(ApifySearchResults<IdealistaSearchRequest> results) {
        Set<IdealistaSearchRequest> residentialProxyRequests = Sets.union(
                IdealistaSearchRequest.withResidentialProxy(results.requestsToRetryDueToEmptyResults()),
                IdealistaSearchRequest.withResidentialProxy(results.requestsToRetryDueToFailure())
        );

        return Sets.union(
                residentialProxyRequests,
                IdealistaSearchRequest.fragment(results.requestsToFragment()));
    }
}
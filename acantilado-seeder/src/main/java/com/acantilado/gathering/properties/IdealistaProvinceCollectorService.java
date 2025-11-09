package com.acantilado.gathering.properties;

import com.acantilado.core.administrative.*;
import com.acantilado.core.idealista.*;
import com.acantilado.core.idealista.realEstate.*;
import com.acantilado.gathering.location.AcantiladoLocationEstablisher;
import com.acantilado.gathering.properties.apify.*;
import com.acantilado.gathering.properties.collectors.*;
import com.acantilado.gathering.properties.idealista.*;
import com.acantilado.gathering.properties.utils.*;
import com.google.common.collect.Sets;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.acantilado.gathering.properties.utils.RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction;

public final class IdealistaProvinceCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaProvinceCollectorService.class);
    private static final Optional<Integer> APIFY_ACTIVE_AGENTS = Optional.of(32);

    private final IdealistaLocationMappingDAO mappingDAO;
    private final IdealistaLocationDAO locationDAO;
    private final SessionFactory sessionFactory;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final AcantiladoLocationEstablisher locationEstablisher;

    private final IdealistaRealEstateCollector<IdealistaTerrain> terrainCollector;
    private final IdealistaRealEstateCollector<IdealistaProperty> propertyCollector;
    private final IdealistaLocationCollector locationCollector;

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
        this.sessionFactory = sessionFactory;

        this.provinceToCollectFor = ProvinceCollectionUtils.getProvinceFromName(
                sessionFactory, provinciaDAO, provinceName);
        this.ayuntamientosForProvince = ProvinceCollectionUtils.getAyuntamientosForProvince(
                sessionFactory, ayuntamientoDAO, provinceToCollectFor);

        locationEstablisher = new AcantiladoLocationEstablisher(
                ProvinceCollectionUtils.getBarriosForProvince(sessionFactory, barrioDAO, provinceToCollectFor),
                ayuntamientosForProvince,
                ProvinceCollectionUtils.getPostcodesForProvince(
                        sessionFactory, codigoPostalDAO, ayuntamientosForProvince),
                ayuntamientoDAO,
                mappingDAO);

        terrainCollector = new IdealistaRealEstateCollector<>(
                locationEstablisher,
                contactDAO,
                terrainDAO,
                IdealistaTerrain::constructFromJson
        );

        propertyCollector = new IdealistaRealEstateCollector<>(
                locationEstablisher,
                contactDAO,
                propertyDAO,
                IdealistaProperty::constructFromJson
        );

        locationCollector = new IdealistaLocationCollector(locationDAO);
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
    public boolean ensureLocationIdsComplete() {
        Set<IdealistaAyuntamientoLocation> locations = ProvinceCollectionUtils.getLocationsForProvince(
                sessionFactory, locationDAO, provinceToCollectFor);
        int locationCount = locations.size();
        int ayuntamientoCount = ayuntamientosForProvince.size();

        if (locationCount >= ayuntamientoCount) {
            LOGGER.info("Location IDs complete: {} location IDs for {} ayuntamientos",
                    locationCount, ayuntamientoCount);
            return true;
        }

        LOGGER.info("Insufficient location IDs {} for ayuntamientos {}; bootstrapping location IDs",
                locationCount, ayuntamientoCount);

        // Bootstrap province-wide to collect location IDs
        startRealEstateCollectionForProvince(
                Set.of(IdealistaSearchRequest.locationBasedSaleSearch(
                        provinceToCollectFor.getIdealistaLocationId(),
                        IdealistaPropertyType.HOMES)),
                locationCollector);

        locations = ProvinceCollectionUtils.getLocationsForProvince(
                sessionFactory, locationDAO, provinceToCollectFor);
        if (locations.size() < ayuntamientoCount) {
            LOGGER.error("Still insufficient location IDs {} for ayuntamientos {}. Manual intervention required.",
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
    public boolean ensureLocationMappingsAreComplete() {
        Set<String> missingLocations = getLocationIdsMissingFromMappings();

        if (missingLocations.isEmpty()) {
            LOGGER.info("All mappings complete for province");
            return true;
        }

        LOGGER.info("Building mappings for {} missing location IDs", missingLocations.size());
        locationEstablisher.setBootstrapMode(true);

        Set<IdealistaSearchRequest> searchRequests = missingLocations.stream()
                .map(locationId -> IdealistaSearchRequest.locationBasedSaleSearch(
                        locationId,
                        IdealistaPropertyType.HOMES))
                .collect(Collectors.toSet());
        startRealEstateCollectionForProvince(searchRequests, propertyCollector);

        RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(
                sessionFactory,
                locationEstablisher::storeInMemoryMappings);

        locationEstablisher.setBootstrapMode(false);
        Set<String> stillMissing = getLocationIdsMissingFromMappings();
        if (!stillMissing.isEmpty()) {
            LOGGER.warn("Could not create mappings for {} location IDs. These locations likely have no listings: {}",
                    stillMissing.size(), stillMissing);
            return false;
        }

        LOGGER.info("Successfully completed Phase 2: All mappings for locations created");
        return true;
    }

    /**
     * Phase 2: Build mappings from Idealista location IDs to Acantilado ayuntamientos
     * @return true if all mappings are complete, false if some couldn't be mapped
     */
    public boolean ensureAyuntamientoMappingsAreComplete() {
        Set<Long> missingAyuntamientoIds = getAyuntamientoIdsMissingFromMappings();
        if (missingAyuntamientoIds.isEmpty()) {
            LOGGER.info("All ayuntamientos are mapped to at least one location ID");
            return true;
        }

        LOGGER.info("After mapping all locationIds, not all ayuntamientos were mapped: {}", missingAyuntamientoIds);

        locationEstablisher.setBootstrapMode(true);
        Set<String> missingAyuntamientos = ayuntamientosForProvince
                .stream()
                .filter(a -> missingAyuntamientoIds.contains(a.getId()))
                .map(Ayuntamiento::getName)
                .collect(Collectors.toSet());
        Set<IdealistaSearchRequest> searchRequests = missingAyuntamientos.stream()
                .map(locationId -> IdealistaSearchRequest.locationBasedSaleSearch(
                        locationId,
                        IdealistaPropertyType.HOMES))
                .collect(Collectors.toSet());
        startRealEstateCollectionForProvince(searchRequests, propertyCollector);

        RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(
                sessionFactory,
                locationEstablisher::storeInMemoryMappings);

        locationEstablisher.setBootstrapMode(false);

        Set<Long> stillMissing = getAyuntamientoIdsMissingFromMappings();
        if (!stillMissing.isEmpty()) {
            LOGGER.warn("Could not create mappings for {} ayuntamiento IDs. These ayuntamientos likely have no listings: {}",
                    stillMissing.size(), stillMissing);
            return false;
        }

        LOGGER.info("Successfully completed Phase 3: All mappings for ayuntamientos created");
        return true;
    }

    /**
     * Phase 3: Collect real estate listings using the complete mappings
     */
    public boolean collectRealEstateListings(IdealistaPropertyType propertyType) {
        // Verify prerequisites
        if (!ensureLocationIdsComplete()) {
            throw new IllegalStateException("Cannot collect listings: Location IDs incomplete");
        }

        if (!getLocationIdsMissingFromMappings().isEmpty()) {
            throw new IllegalStateException("Cannot collect listings: Mappings incomplete");
        }

        LOGGER.info("Starting Phase 4: Collecting {} listings for province", propertyType);

        // Create search requests for each ayuntamiento using mappings
        Set<IdealistaSearchRequest> searchRequests = ayuntamientosForProvince
                .stream()
                .map(ayuntamiento -> createSearchRequest(ayuntamiento, propertyType))
                .collect(Collectors.toSet());

        IdealistaRealEstateCollector<?> collector = switch (propertyType) {
            case HOMES -> propertyCollector;
            case LANDS -> terrainCollector;
        };

        boolean success = startRealEstateCollectionForProvince(searchRequests, collector);

        if (success) {
            LOGGER.info("Successfully completed Phase 4. Collection stats: {}",
                    locationEstablisher.getCollectionStats());
        }

        return success;
    }

    /**
     * Orchestrate all three phases in sequence.
     * This is your "do everything" method, but now it's explicit about phases.
     */
    public boolean collectRealEstateForProvince(IdealistaPropertyType propertyType) {
        LOGGER.info("Starting full collection workflow for province: {}",
                provinceToCollectFor.getName());

        // Phase 1: Location IDs
        if (!ensureLocationIdsComplete()) {
            LOGGER.error("Phase 1 failed: Could not collect all location IDs. Manual intervention required.");
            return false;
        }

        // Phase 2: Mappings for all location IDs
        if (!ensureLocationMappingsAreComplete()) {
            LOGGER.error("Phase 2 failed: Could not create mappings for all locations. Manual intervention required.");
            return false;
        }

        // Phase 3: Mapping for all ayuntamientos
        if (!ensureAyuntamientoMappingsAreComplete()) {
            LOGGER.error("Phase 3 failed: Could not create mappings for all ayuntamientos. Manual intervention required.");
            return false;
        }

        // Phase 4: Start regular listing collection
        return collectRealEstateListings(propertyType);
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
    private Set<Long> getAyuntamientoIdsMissingFromMappings() {
        Set<String> allLocationIds = ProvinceCollectionUtils
                .getLocationsForProvince(sessionFactory, locationDAO, provinceToCollectFor)
                .stream()
                .map(IdealistaAyuntamientoLocation::getAyuntamientoLocationId)
                .collect(Collectors.toSet());

        Set<Long> allAyuntamientoIds = ayuntamientosForProvince
                .stream()
                .map(Ayuntamiento::getId)
                .collect(Collectors.toSet());

        Set<Long> mappedAyuntamientoIds = ProvinceCollectionUtils
                .getMappingsByAyuntamientoIds(sessionFactory, mappingDAO, allLocationIds)
                .keySet();

        Set<Long> missingIds = new HashSet<>(allAyuntamientoIds);
        missingIds.removeAll(mappedAyuntamientoIds);

        return missingIds;
    }

    private IdealistaSearchRequest createSearchRequest(Ayuntamiento ayuntamiento, IdealistaPropertyType propertyType) {
        Optional<IdealistaLocationMapping> mapping = executeCallableInSessionWithoutTransaction(
                sessionFactory,
                () -> {
                    List<IdealistaLocationMapping> results = mappingDAO.findByAyuntamientoId(ayuntamiento.getId());
                    if (results.size() != 1) {
                        throw new RuntimeException("Expected exactly one mapping for ayuntamiento "
                                + ayuntamiento.getId() + " but found " + results.size());
                    }
                    return Optional.of(results.get(0));
                });

        if (mapping.isPresent()) {
            LOGGER.debug("Using location ID {} for ayuntamiento {}",
                    mapping.get().getIdealistaLocationId(), ayuntamiento.getName());
            return IdealistaSearchRequest.saleSearch(
                    mapping.get().getIdealistaLocationId(),
                    propertyType
            );
        } else {
            LOGGER.warn("No mapping found for ayuntamiento {}, using name-based search",
                    ayuntamiento.getName());
            return IdealistaSearchRequest.saleSearch(
                    ayuntamiento.getName(),
                    propertyType
            );
        }
    }

    private <T> boolean startRealEstateCollectionForProvince(
            Set<IdealistaSearchRequest> searchRequests,
            ApifyCollector<T> collector) {
        LOGGER.info("Triggering {} search requests", searchRequests.size());
        Set<ApifyRunningSearch> pendingSearches = triggerSearches(searchRequests, collector);
        LOGGER.info("Finished triggering {} search requests", pendingSearches.size());

        LOGGER.info("Awaiting completion of {} searches", pendingSearches.size());
        Set<ApifyRunningSearch> finishedSearches = awaitSearchesFinishing(pendingSearches, collector);
        LOGGER.info("Confirmed {} searches have finished", finishedSearches.size());

        Set<IdealistaSearchRequest> requestsToBeRetried = new HashSet<>();
        finishedSearches.forEach(finishedSearch -> {
            if (finishedSearch.pendingSearchStatus().equals(ApifySearchStatus.FAILED)) {
                requestsToBeRetried.add(finishedSearch.request());
            }
        });

        LOGGER.debug("Storing results from {} completed searches", finishedSearches.size());
        Set<IdealistaSearchRequest> requestsToBeFragmented = storeSearchResults(finishedSearches, collector);

        if (!requestsToBeRetried.isEmpty()) {
            LOGGER.warn("{} requests failed and will be retried", requestsToBeRetried.size());
        }

        Set<IdealistaSearchRequest> fragmentedRequests = new HashSet<>();
        if (!requestsToBeFragmented.isEmpty()) {
            fragmentedRequests = IdealistaSearchRequest.fragment(requestsToBeFragmented);
            if (fragmentedRequests.isEmpty()) {
                LOGGER.error("Cannot fragment requests: {}", requestsToBeFragmented);
                return false;
            }

            LOGGER.warn("{} searches exceeded Idealista limit and will be fragmented into {} requests",
                    requestsToBeFragmented.size(),
                    fragmentedRequests.size());
        }

        Set<IdealistaSearchRequest> remainingRequestsToRun = Sets.union(requestsToBeRetried, fragmentedRequests);
        if (!remainingRequestsToRun.isEmpty()) {
            LOGGER.info("Recursively processing {} remaining requests", remainingRequestsToRun.size());
            return startRealEstateCollectionForProvince(remainingRequestsToRun, collector);
        }

        return true;
    }

    private <T> Set<ApifyRunningSearch> triggerSearches(
            Set<IdealistaSearchRequest> toRun,
            ApifyCollector<T> collector) {
        return executeIteratively(APIFY_ACTIVE_AGENTS, toRun, collector::startSearch);
    }

    private <T> Set<ApifyRunningSearch> awaitSearchesFinishing(
            Set<ApifyRunningSearch> pendingSearches,
            ApifyCollector<T> collector) {
        return executeIteratively(APIFY_ACTIVE_AGENTS, pendingSearches, (ApifyRunningSearch pendingSearch) -> {
            ApifySearchStatus searchStatus = collector.getSearchStatus(pendingSearch);
            if (searchStatus.isFinished()) {
                return pendingSearch.withStatus(searchStatus);
            }
            return null;
        });
    }

    private <T> Set<IdealistaSearchRequest> storeSearchResults(
            Set<ApifyRunningSearch> finishedSearches,
            ApifyCollector<T> collector) {
        Set<IdealistaSearchRequest> requestsToFragment = ConcurrentHashMap.newKeySet();

        // Process sequentially to avoid deadlocks from concurrent identical property storage
        for (ApifyRunningSearch search : finishedSearches) {
            try {
                RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(sessionFactory, () -> {
                    Set<T> realEstates = collector.getSearchResults(search);

                    if (realEstates.isEmpty()) {
                        LOGGER.warn("No results for search: {}", search.request().getLocation());
                    }

                    if (realEstates.size() > 2000) {
                        requestsToFragment.add(search.request());
                    }

                    realEstates.forEach(collector::storeResult);
                    LOGGER.info("Stored batch of {} results", realEstates.size());
                });
            } catch (Exception e) {
                LOGGER.error("Failed to store results for search: {}", search, e);
            }
        }

        LOGGER.info("Stored results from {} searches; {} need fragmentation",
                finishedSearches.size(),
                requestsToFragment.size());

        return requestsToFragment;
    }

    private <S, T> Set<T> executeIteratively(Optional<Integer> batchSize, Set<S> toRun,
                                             Function<S, T> resultOrNullFunction) {
        return RetryableBatchedExecutor.executeUntilAllSuccessful(
                toRun, resultOrNullFunction, batchSize, executorService);
    }
}
package com.acantilado.collection.properties;

import com.acantilado.collection.location.AcantiladoLocationEstablisher;
import com.acantilado.collection.properties.apify.ApifyRunningSearch;
import com.acantilado.collection.properties.apify.ApifySearchStatus;
import com.acantilado.collection.properties.collectors.ApifyCollector;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.acantilado.utils.RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction;

public final class IdealistaProvinceCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaProvinceCollectorService.class);
    private static final Optional<Integer> APIFY_ACTIVE_AGENTS = Optional.of(32);

    public record searchResults(
            Set<IdealistaSearchRequest> requestsSucceeded,
            Set<IdealistaSearchRequest> requestsToFragment,
            Set<IdealistaSearchRequest> requestsToRetryDueToFailure,
            Set<IdealistaSearchRequest> requestsToRetryDueToProxy) {

        @Override
        public String toString() {
            return "requestsSucceeded: " + requestsSucceeded +
                    "; requestsToFragment " + requestsToFragment +
                    "; requestsToRetryDueToFailure " + requestsToRetryDueToFailure +
                    "; requestsToRetryDueToProxy " + requestsToRetryDueToProxy;
        }
    }

    private final IdealistaLocationMappingDAO mappingDAO;
    private final IdealistaLocationDAO locationDAO;

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
    private boolean bootstrapLocationMappings() {
        Set<String> missingLocations = getLocationIdsMissingFromMappings();

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
            LOGGER.warn("Could not create mappings for {} location IDs: {}",
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
    private boolean bootstrapAyuntamientoMappings() {
        Set<Long> missingAyuntamientoIds = getAyuntamientoIdsMissingFromMappings();

        LOGGER.info("Building mappings for {} missing ayuntamientos", missingAyuntamientoIds.size());
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
        LOGGER.info("Starting Phase 5: Collecting {} listings for province", propertyType);

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
            LOGGER.info("Successfully completed Phase 5. Collection stats: {}",
                    locationEstablisher.getCollectionStats());
        }

        return success;
    }

    public boolean collectRealEstateForProvince(IdealistaPropertyType propertyType) {
        LOGGER.info("Starting full collection workflow for province {} and property type {}",
                provinceToCollectFor.getName(), propertyType);

        if (locationMappingMerchant.importMappings(provinceToCollectFor)) {
            LOGGER.info("Imported mappings from disk");
        }

        Set<String> missingLocationMappings = getLocationIdsMissingFromMappings();
        Set<Long> missingAyuntamientoMappings = getAyuntamientoIdsMissingFromMappings();

        if (missingLocationMappings.isEmpty() && missingAyuntamientoMappings.isEmpty()) {
            LOGGER.info("All mappings complete, skipping bootstrap phases");
            return collectRealEstateListings(propertyType);
        }
        LOGGER.info("Mappings incomplete: {} location IDs and {} ayuntamientos need mapping",
                missingLocationMappings.size(), missingAyuntamientoMappings.size());

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

        // Phase 5: Start regular listing collection
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

        searchResults results = storeSearchResults(finishedSearches, collector);
        LOGGER.info("Search results: {}", results);

        Set<IdealistaSearchRequest> remainingRequestsToRun = calculateRequestsToRetry(results);
        if (!remainingRequestsToRun.isEmpty()) {
            LOGGER.info("Processing {} remaining requests", remainingRequestsToRun.size());
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

    private <T> searchResults storeSearchResults(
            Set<ApifyRunningSearch> finishedSearches,
            ApifyCollector<T> collector) {
        Set<IdealistaSearchRequest> requestsSucceeded = ConcurrentHashMap.newKeySet();
        Set<IdealistaSearchRequest> requestsToFragment = ConcurrentHashMap.newKeySet();
        Set<IdealistaSearchRequest> requestsToRetryDueToProxy = ConcurrentHashMap.newKeySet();
        Set<IdealistaSearchRequest> requestsToRetryDueToFailure = ConcurrentHashMap.newKeySet();

        LOGGER.info("Storing search results for {} finished searches", finishedSearches.size());

        // Process sequentially to avoid deadlocks from concurrent identical property storage
        for (ApifyRunningSearch search : finishedSearches) {
            try {
                RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(sessionFactory, () -> {
                    if (search.pendingSearchStatus().equals(ApifySearchStatus.FAILED)) {
                        requestsToRetryDueToFailure.add(search.request());
                        return;
                    }

                    Set<T> realEstates = collector.getSearchResults(search);
                    if (realEstates.isEmpty()) {
                        requestsToRetryDueToProxy.add(search.request());
                        LOGGER.warn("No results for search, will be retried with residential proxy: {}", search.request().getLocation());
                        return;
                    }

                    if (realEstates.size() > 2300) {
                        requestsToFragment.add(search.request());
                        return;
                    }

                    requestsSucceeded.add(search.request());
                    realEstates.forEach(collector::storeResult);
                    LOGGER.debug("Stored batch of {} results", realEstates.size());
                });
            } catch (Exception e) {
                LOGGER.error("Failed to store results for search: {}", search, e);
            }
        }

        return new searchResults(requestsSucceeded, requestsToFragment, requestsToRetryDueToFailure, requestsToRetryDueToProxy);
    }

    private <S, T> Set<T> executeIteratively(Optional<Integer> batchSize, Set<S> toRun,
                                             Function<S, T> resultOrNullFunction) {
        return RetryableBatchedExecutor.executeUntilAllSuccessful(
                toRun, resultOrNullFunction, batchSize, executorService);
    }

    private static Set<IdealistaSearchRequest> calculateRequestsToRetry(searchResults results) {
        Set<IdealistaSearchRequest> fragmentedRequests = new HashSet<>();
        if (!results.requestsToFragment.isEmpty()) {
            fragmentedRequests = IdealistaSearchRequest.fragment(results.requestsToFragment);
            if (fragmentedRequests.isEmpty()) {
                LOGGER.error("Cannot further fragment requests: {}", results.requestsToFragment);
            }

            LOGGER.warn("{} searches exceeded Idealista limit and will be fragmented into {} requests",
                    results.requestsToFragment.size(),
                    fragmentedRequests.size());
        }

        Set<IdealistaSearchRequest> residentialProxyRequests = new HashSet<>();
        if (!results.requestsToRetryDueToProxy.isEmpty()) {
            residentialProxyRequests = IdealistaSearchRequest.withResidentialProxy(results.requestsToRetryDueToProxy);
        }

        return Sets.union(
                Sets.union(results.requestsToRetryDueToFailure, residentialProxyRequests),
                fragmentedRequests);
    }
}
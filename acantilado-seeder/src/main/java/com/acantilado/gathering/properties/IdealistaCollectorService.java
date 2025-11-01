package com.acantilado.gathering.properties;

import com.acantilado.core.administrative.*;
import com.acantilado.core.idealista.IdealistaContactInformationDAO;
import com.acantilado.core.idealista.IdealistaLocationDAO;
import com.acantilado.core.idealista.IdealistaPropertyDAO;
import com.acantilado.core.idealista.IdealistaTerrainDAO;
import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import com.acantilado.gathering.location.AcantiladoLocationEstablisher;
import com.acantilado.gathering.properties.apify.ApifyRunningSearch;
import com.acantilado.gathering.properties.apify.ApifySearchStatus;
import com.acantilado.gathering.properties.collectors.ApifyCollector;
import com.acantilado.gathering.properties.collectors.IdealistaLocationCollector;
import com.acantilado.gathering.properties.collectors.IdealistaRealEstateCollector;
import com.acantilado.gathering.properties.idealista.IdealistaPropertyType;
import com.acantilado.gathering.properties.idealista.IdealistaSearchRequest;
import com.acantilado.gathering.properties.utils.RetryableBatchedExecutor;
import com.google.common.collect.Sets;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
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

public final class IdealistaCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollectorService.class);
    private static final Optional<Integer> APIFY_ACTIVE_AGENTS = Optional.of(32);

    private final ProvinciaDAO provinciaDao;
    private final AyuntamientoDAO ayuntamientoDAO;
    private final IdealistaLocationMappingDAO mappingDAO;
    private final IdealistaLocationDAO locationDAO;

    private final SessionFactory sessionFactory;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final AcantiladoLocationEstablisher locationEstablisher;

    private final IdealistaRealEstateCollector<IdealistaTerrain> terrainCollector;
    private final IdealistaRealEstateCollector<IdealistaProperty> propertyCollector;
    private final IdealistaLocationCollector locationCollector;

    public IdealistaCollectorService(
            IdealistaContactInformationDAO contactDAO,
            IdealistaPropertyDAO propertyDAO,
            IdealistaTerrainDAO terrainDAO,
            IdealistaLocationDAO locationDAO,
            ProvinciaDAO provinciaDao,
            CodigoPostalDAO codigoPostalDAO,
            AyuntamientoDAO ayuntamientoDAO,
            BarrioDAO barrioDAO,
            IdealistaLocationMappingDAO mappingDAO,
            SessionFactory sessionFactory) {
        this.provinciaDao = provinciaDao;
        this.ayuntamientoDAO = ayuntamientoDAO;
        this.mappingDAO = mappingDAO;
        this.locationDAO = locationDAO;
        this.sessionFactory = sessionFactory;

        locationEstablisher = new AcantiladoLocationEstablisher(
                getMappedLocationIds(),
                provinciaDao,
                ayuntamientoDAO,
                barrioDAO,
                codigoPostalDAO,
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

    public boolean collectRealEstateForProvince(String provinceName, IdealistaPropertyType propertyType) {
        Provincia provincia = getProvinceFromName(provinciaDao, sessionFactory, provinceName);
        Set<Ayuntamiento> ayuntamientosForProvince = getAyuntamientosForProvince(ayuntamientoDAO, sessionFactory, provincia.getId());
        Set<IdealistaAyuntamientoLocation> locationsForProvince = getLocationsForProvince(locationDAO, sessionFactory, provincia.getId());

        if (ayuntamientosForProvince.size() > locationsForProvince.size()) {
            LOGGER.info("Insufficient location IDs {} found for ayuntamientos {} in province {}; seeding location IDs",
                    locationsForProvince.size(),
                    ayuntamientosForProvince.size(),
                    provincia.getIdealistaLocationId());
        }

        bootstrapProvince(Set.of(provincia.getIdealistaLocationId()));
        locationsForProvince = getLocationsForProvince(locationDAO, sessionFactory, provincia.getId());
        if (ayuntamientosForProvince.size() > locationsForProvince.size()) {
            LOGGER.info("Still insufficient location IDs {} for ayuntamientos {} for province {}. Ones found: {}",
                    locationsForProvince.size(),
                    ayuntamientosForProvince.size(),
                    provincia.getIdealistaLocationId(),
                    locationsForProvince);
        }

        return true;

        /* TODO
        * Save a set of properties instead of one by one, for a single search batch
        * Reintroduce location establisher
        * Reorchestrate logic for locations/mappings/listings
         */

//        Set<String> locationIdsToPopulate = locationEstablisher.locationsToPopulate(sessionFactory, provinceName);
//
//        if (!locationIdsToPopulate.isEmpty()) {
//            locationEstablisher.setBootstrapMode(true);
//            boolean success = bootstrapProvince(locationIdsToPopulate);
//
//            RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(sessionFactory, locationEstablisher::storeMappings);
//
//            return success;
//        }
//
//        return collectForProvinceAyuntamientos(provinceName, propertyType);
    }

    private Set<String> getMappedLocationIds() {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);

        try {
            return mappingDAO.findAll()
                    .stream()
                    .map(IdealistaLocationMapping::getIdealistaLocationId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    private boolean collectForProvinceAyuntamientos(String provinceName, IdealistaPropertyType propertyType) {
        List<Ayuntamiento> ayuntamientos = getAyuntamientosForProvince(provinceName);
        Set<IdealistaSearchRequest> searchRequests = ayuntamientos
                .stream()
                .map(ayuntamiento -> createSearchRequest(ayuntamiento, propertyType))
                .collect(Collectors.toSet());

        return switch (propertyType) {
            case HOMES -> startRealEstateCollectionForProvince(searchRequests, propertyCollector);
            case LANDS -> startRealEstateCollectionForProvince(searchRequests, terrainCollector);
        };
    }

    private boolean bootstrapProvince(Set<String> locations) {
        Set<IdealistaSearchRequest> searchRequests = new HashSet<>();
        locations.forEach(location -> {
            // Homes are more productive because of request fragmentation based on surface area.
            searchRequests.add(IdealistaSearchRequest.locationBasedSaleSearch(location, IdealistaPropertyType.HOMES));
        });

        return startRealEstateCollectionForProvince(searchRequests, locationCollector);
    }

    private IdealistaSearchRequest createSearchRequest(Ayuntamiento ayuntamiento, IdealistaPropertyType propertyType) {
        Optional<IdealistaLocationMapping> mapping = RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction(
                        sessionFactory,
                        () -> {
                            List<IdealistaLocationMapping> results = mappingDAO.findByAyuntamientoId(ayuntamiento.getId());
                            if (results.size() != 1) {
                                throw new RuntimeException("More than one result found for " + ayuntamiento.getId());
                            }
                            return Optional.of(results.get(0));
                        });

        if (mapping.isPresent()) {
            LOGGER.info("Using location ID {} for ayuntamiento {}",
                    mapping.get().getIdealistaLocationId(), ayuntamiento.getName());
            return IdealistaSearchRequest.saleSearch(
                    mapping.get().getIdealistaLocationId(),
                    propertyType
            );
        } else {
            LOGGER.info("No mapping found, using ayuntamiento name {} for search", ayuntamiento.getName());
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
        LOGGER.info("Finished {} search requests", pendingSearches.size());

        LOGGER.info("Sending {} status requests", pendingSearches.size());
        Set<ApifyRunningSearch> finishedSearches = awaitSearchesFinishing(pendingSearches, collector);
        LOGGER.info("Confirmed {} requests have finished", finishedSearches.size());

        Set<IdealistaSearchRequest> requestsToBeRetried = new HashSet<>();
        finishedSearches.forEach(finishedSearch -> {
            if (finishedSearch.pendingSearchStatus().equals(ApifySearchStatus.FAILED)) {
                requestsToBeRetried.add(finishedSearch.request());
            }
        });

        LOGGER.debug("Storing completed searches {}", finishedSearches);
        Set<IdealistaSearchRequest> requestsToBeFragmented = storeSearchResults(finishedSearches, collector);

        if (!requestsToBeRetried.isEmpty()) {
            LOGGER.warn("{} requests failed to execute and will be retried", requestsToBeRetried.size());
        }

        Set<IdealistaSearchRequest> fragmentedRequests = new HashSet<>();
        if (!requestsToBeFragmented.isEmpty()) {
            fragmentedRequests = IdealistaSearchRequest.fragment(requestsToBeFragmented);
            if (fragmentedRequests.isEmpty()) {
                LOGGER.error("Unable to (further) fragment {} requests that need to be split", requestsToBeFragmented);
                return false;
            }

            LOGGER.warn("Some searches {} exceeded Idealista limit and will need to be fragmented into {}",
                    requestsToBeFragmented,
                    fragmentedRequests.size());
        }

        Set<IdealistaSearchRequest> remainingRequestsToRun = Sets.union(requestsToBeRetried, fragmentedRequests);
        if (!remainingRequestsToRun.isEmpty()) {
            startRealEstateCollectionForProvince(remainingRequestsToRun, collector);
        }

        LOGGER.info("Real estate collection complete: {} ads processed, " +
                        "{} postcodes required brute force matching, " +
                        "{} barrios were successfully matched and {} were missed.",
                locationEstablisher.getLocationsEstablished(),
                locationEstablisher.getBruteForceCodigoPostalMatches(),
                locationEstablisher.getBarriosEstablished(),
                locationEstablisher.getBarrioMisses());
        return true;
    }

    private List<Ayuntamiento> getAyuntamientosForProvince(String provinceName) {
        return RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            List<Provincia> provinces = provinciaDao.findByName(provinceName);
            if (provinces.size() != 1) {
                LOGGER.error("Got 0 or >1 hits for a province name, this is unexpected {} {}", provinceName, provinces);
                return List.of();
            }
            List<Ayuntamiento> ayuntamientos = ayuntamientoDAO.findByProvinceId(provinces.get(0).getId());
            if (ayuntamientos.isEmpty()) {
                LOGGER.error("No ayuntamientos found for province name {}", provinceName);
                return List.of();
            }
            return ayuntamientos;
        });
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

    // This has to be single threaded to avoid deadlocks from storing identical properties from different batches.
    private <T> Set<IdealistaSearchRequest> storeSearchResults(
            Set<ApifyRunningSearch> finishedSearches,
            ApifyCollector<T> collector) {
        Set<IdealistaSearchRequest> requestsToFragment = ConcurrentHashMap.newKeySet();

        // Process sequentially to avoid deadlocks
        for (ApifyRunningSearch search : finishedSearches) {
            try {
                RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(sessionFactory, () -> {
                    Set<T> realEstates = collector.getSearchResults(search);
                    if (realEstates.isEmpty()) {
                        LOGGER.error("No results for search {}", search.request().getLocation());
                    }

                    if (realEstates.size() > 2000) {
                        requestsToFragment.add(search.request());
                    }

                    realEstates.forEach(collector::storeResult);
                });
            } catch (Exception e) {
                LOGGER.error("Failed to store search results for {}", search, e);
            }
        }

        LOGGER.info("Stored {} requests; {} have to be fragmented",
                finishedSearches.size(),
                requestsToFragment.size());

        return requestsToFragment;
    }

    private <S, T> Set<T> executeIteratively(Optional<Integer> batchSize, Set<S> toRun, Function<S, T> resultOrNullFunction) {
        return RetryableBatchedExecutor.executeUntilAllSuccessful(toRun, resultOrNullFunction, batchSize, executorService);
    }

    private static Provincia getProvinceFromName(ProvinciaDAO provinciaDao, SessionFactory sessionFactory, String name) {
        return RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            List<Provincia> provincias = provinciaDao.findByName(name);
            if (provincias.size() != 1) {
                throw new RuntimeException("More than 1 or 0 provinces found for province " + name);
            }
            return provincias.get(0);
        });
    }

    private static Set<Ayuntamiento> getAyuntamientosForProvince(AyuntamientoDAO ayuntamientoDAO, SessionFactory sessionFactory, long provinceId) {
        return RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction(sessionFactory,
                () -> new HashSet<>(ayuntamientoDAO.findByProvinceId(provinceId)));
    }

    private static Set<IdealistaAyuntamientoLocation> getLocationsForProvince(IdealistaLocationDAO locationDAO, SessionFactory sessionFactory, long provinceId) {
        return RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction(sessionFactory,
                () -> new HashSet<>(locationDAO.findByProvinceId(provinceId)));
    }
}
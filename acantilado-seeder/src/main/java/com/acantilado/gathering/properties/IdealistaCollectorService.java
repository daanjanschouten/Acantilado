package com.acantilado.gathering.properties;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.Provincia;
import com.acantilado.core.administrative.ProvinciaDAO;
import com.acantilado.core.idealista.*;
import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.core.idealista.realEstate.IdealistaRealEstate;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import com.acantilado.gathering.properties.collectors.ApifyCollector;
import com.acantilado.gathering.properties.collectors.IdealistaPropertyCollector;
import com.acantilado.gathering.properties.collectors.IdealistaTerrainCollector;
import com.acantilado.gathering.properties.collectors.PendingSearchOrError;
import com.acantilado.gathering.properties.idealistaTypes.IdealistaPropertyType;
import com.acantilado.gathering.properties.idealistaTypes.IdealistaSearchRequest;
import com.acantilado.gathering.utils.HibernateUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.acantilado.gathering.properties.IdealistaDeduplicationUtils.establishContactInformation;

public final class IdealistaCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollectorService.class);

    private static final Optional<Integer> APIFY_ACTIVE_AGENTS = Optional.of(32);

    private final IdealistaContactInformationDAO contactDAO;
    private final IdealistaPropertyDAO propertyDAO;
    private final IdealistaTerrainDAO terrainDAO;
    private final ProvinciaDAO provinciaDao;
    private final AyuntamientoDAO ayuntamientoDao;
    private final SessionFactory sessionFactory;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ApifyCollector<IdealistaTerrain> terrainCollector = new IdealistaTerrainCollector();
    private final ApifyCollector<IdealistaProperty> propertyCollector = new IdealistaPropertyCollector();

    public IdealistaCollectorService(
            IdealistaContactInformationDAO contactDAO,
            IdealistaPropertyDAO propertyDAO,
            IdealistaTerrainDAO terrainDAO,
            ProvinciaDAO provinciaDao,
            AyuntamientoDAO ayuntamientoDao,
            SessionFactory sessionFactory) {
        this.contactDAO = contactDAO;
        this.propertyDAO = propertyDAO;
        this.terrainDAO = terrainDAO;
        this.provinciaDao = provinciaDao;
        this.ayuntamientoDao = ayuntamientoDao;
        this.sessionFactory = sessionFactory;
    }

    public record IdealistaRealEstateResult<T extends IdealistaRealEstate<?>>(T idealistaRealEstate, Result result) {
        public enum Result { EXISTING_IDENTICAL, PRICE_CHANGE, NEW }
    }

    public record IdealistaCollectorAndDao<T extends IdealistaRealEstate<?>>(
            ApifyCollector<T> collector,
            IdealistaRealEstateDAO<T> realEstateDao
    ) {}

    public boolean collectRealEstateForProvinceName(String provinceName, IdealistaPropertyType propertyType) {
        Set<String> ayuntamientosForProvince = getAyuntamientosForProvince(provinceName)
                .stream()
                .map(Ayuntamiento::getName)
                .collect(Collectors.toSet());

        IdealistaSearchRequest searchRequest = ayuntamientosForProvince
                .stream()
                .map(ayuntamiento -> IdealistaSearchRequest.saleSearch(ayuntamiento, propertyType))
                .findFirst().get();
        Set<IdealistaSearchRequest> searchRequests = Set.of(searchRequest);

        return switch (propertyType) {
            case HOMES -> startRealEstateCollectionForProvince(
                    searchRequests,
                    new IdealistaCollectorAndDao<>(propertyCollector, propertyDAO));
            case LANDS -> startRealEstateCollectionForProvince(
                    searchRequests,
                    new IdealistaCollectorAndDao<>(terrainCollector, terrainDAO));
        };
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

    private <T extends IdealistaRealEstate<?>> boolean startRealEstateCollectionForProvince(
            Set<IdealistaSearchRequest> searchRequests,
            IdealistaCollectorAndDao<T> collectorAndDao) {
        LOGGER.info("Triggering {} search requests", searchRequests.size());
        Set<ApifyCollector.ApifyPendingSearch> pendingSearches = triggerSearches(searchRequests, collectorAndDao);
        LOGGER.info("Finished {} search requests", pendingSearches.size());

        LOGGER.info("Sending {} status requests", pendingSearches.size());
        Set<ApifyCollector.ApifyPendingSearch> finishedSearches = awaitSearchesFinishing(pendingSearches, collectorAndDao);
        LOGGER.info("Confirmed {} requests have finished", finishedSearches.size());

        LOGGER.info("Storing completed searches");
        Set<IdealistaSearchRequest> requestsToBeFragmented = storeSearchResults(finishedSearches, collectorAndDao);

        if (!requestsToBeFragmented.isEmpty()) {
            Set<IdealistaSearchRequest> fragmentedRequests = IdealistaSearchRequest.fragment(requestsToBeFragmented);
            if (fragmentedRequests.isEmpty()) {
                LOGGER.error("Unable to (further) fragment {} requests - they exceeded limits but cannot be split",
                        requestsToBeFragmented);
                return false;
            }

            LOGGER.warn("Some searches {} exceeded Idealista limit and will need to be fragmented into {}",
                    requestsToBeFragmented,
                    fragmentedRequests.size());

            startRealEstateCollectionForProvince(fragmentedRequests, collectorAndDao);
        } else {
            LOGGER.info("Property collection complete");
        }
        return true;
    }

    private List<Ayuntamiento> getAyuntamientosForProvince(String provinceName) {
        return HibernateUtils.executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            List<Provincia> provinces = provinciaDao.findByName(provinceName);
            if (provinces.size() != 1) {
                LOGGER.error("Got 0 or >1 hits for a province name, this is unexpected {} {}", provinceName, provinces);
                return List.of();
            }
            List<Ayuntamiento> ayuntamientos = ayuntamientoDao.findByProvinceId(provinces.get(0).getId());
            if (ayuntamientos.isEmpty()) {
                LOGGER.error("No ayuntamientos found for province name {}", provinceName);
                return List.of();
            }
            return ayuntamientos;
        });
    }

    private <T extends IdealistaRealEstate<?>> Set<ApifyCollector.ApifyPendingSearch> triggerSearches(
            Set<IdealistaSearchRequest> toRun,
            IdealistaCollectorAndDao<T> collectorAndDao) {
        return executeIteratively(APIFY_ACTIVE_AGENTS, toRun, (IdealistaSearchRequest searchRequest) -> {
            PendingSearchOrError result = collectorAndDao.collector.startSearch(searchRequest);
            if (!result.isSucceeded()) {
                LOGGER.debug("Request failed with error {}", result.getError().get());
                return null;
            }
            return result.getPendingSearch().get();
        });
    }

    private <T extends IdealistaRealEstate<?>> Set<ApifyCollector.ApifyPendingSearch> awaitSearchesFinishing(
            Set<ApifyCollector.ApifyPendingSearch> pendingSearches,
            IdealistaCollectorAndDao<T> collectorAndDao) {
        return executeIteratively(APIFY_ACTIVE_AGENTS, pendingSearches, (ApifyCollector.ApifyPendingSearch pendingSearch) -> {
            if (collectorAndDao.collector.getSearchStatus(pendingSearch) == ApifyCollector.PENDING_SEARCH_STATUS.SUCCEEDED) {
                return pendingSearch;
            }
            return null;
        });
    }

    // This has to be single threaded to avoid deadlocks from storing identical properties from different batches.
    private <T extends IdealistaRealEstate<?>> Set<IdealistaSearchRequest> storeSearchResults(
            Set<ApifyCollector.ApifyPendingSearch> finishedSearches,
            IdealistaCollectorAndDao<T> collectorAndDao) {
        Set<IdealistaSearchRequest> requestsToFragment = ConcurrentHashMap.newKeySet();

        // Process sequentially to avoid deadlocks
        for (ApifyCollector.ApifyPendingSearch search : finishedSearches) {
            try {
                HibernateUtils.executeRunnableInSessionWithTransaction(sessionFactory, () -> {
                    Set<T> realEstates = collectorAndDao.collector.getSearchResults(search);

                    if (realEstates.size() > 2000) {
                        requestsToFragment.add(search.request());
                    }

                    realEstates.forEach(realEstate -> {
                        processProperty(realEstate, contactDAO, collectorAndDao);
                    });
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

    public static <T extends IdealistaRealEstate<?>> IdealistaRealEstateResult<T> establishProperty(
            T newRealEstate,
            IdealistaCollectorService.IdealistaCollectorAndDao<T> collectorAndDao) {
        final long code = newRealEstate.getPropertyCode();
        Optional<T> maybeRealEstate = collectorAndDao.realEstateDao.findByPropertyCode(code);


        return maybeRealEstate.map(
                existingRealEstate -> IdealistaDeduplicationUtils.mergeRealEstate(newRealEstate, existingRealEstate, code))
                .orElseGet(() -> new IdealistaRealEstateResult<>(newRealEstate, IdealistaRealEstateResult.Result.NEW));
    }

    public static <T extends IdealistaRealEstate<?>> void processProperty(
            T realEstate,
            IdealistaContactInformationDAO contactInformationDAO,
            IdealistaCollectorService.IdealistaCollectorAndDao<T> collectorAndDao) {
        IdealistaContactInformation definitiveContactInformation = establishContactInformation(realEstate.getContactInfo(), contactInformationDAO);
        IdealistaRealEstateResult<T> idealistaRealEstateResult = establishProperty(realEstate, collectorAndDao);
        T definitiveIdealistaRealEstate = idealistaRealEstateResult.idealistaRealEstate;

        definitiveIdealistaRealEstate.setContactInfo(definitiveContactInformation);
        collectorAndDao.realEstateDao.saveOrUpdate(definitiveIdealistaRealEstate);
    }

    private <S, T> Set<T> executeIteratively(Optional<Integer> batchSize, Set<S> toRun, Function<S, T> resultOrNullFunction) {
        return HibernateUtils.executeUntilAllSuccessful(toRun, resultOrNullFunction, batchSize, executorService);
    }
}
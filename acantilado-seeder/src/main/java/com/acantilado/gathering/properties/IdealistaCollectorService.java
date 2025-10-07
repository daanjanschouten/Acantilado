package com.acantilado.gathering.properties;


import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDao;
import com.acantilado.core.administrative.Provincia;
import com.acantilado.core.administrative.ProvinciaDao;
import com.acantilado.core.properties.idealista.*;
import com.acantilado.gathering.properties.collectors.ApifyCollector;
import com.acantilado.gathering.properties.collectors.IdealistaCollector;
import com.acantilado.gathering.properties.collectors.PendingSearchOrError;
import com.acantilado.gathering.properties.idealistaTypes.IdealistaPropertyType;
import com.acantilado.gathering.properties.idealistaTypes.IdealistaSearchRequest;
import com.acantilado.gathering.utils.HibernateUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class IdealistaCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollectorService.class);

    private static final Optional<Integer> APIFY_ACTIVE_AGENTS = Optional.of(32);

    private final IdealistaContactInformationDAO contactDAO;
    private final IdealistaPropertyDAO propertyDAO;
    private final IdealistaPriceRecordDAO priceRecordDAO;
    private final ProvinciaDao provinciaDao;
    private final AyuntamientoDao ayuntamientoDao;
    private final SessionFactory sessionFactory;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final IdealistaCollector collector = new IdealistaCollector();

    public IdealistaCollectorService(
            IdealistaContactInformationDAO contactDAO,
            IdealistaPropertyDAO propertyDAO,
            IdealistaPriceRecordDAO priceRecordDAO,
            ProvinciaDao provinciaDao,
            AyuntamientoDao ayuntamientoDao,
            SessionFactory sessionFactory) {
        this.contactDAO = contactDAO;
        this.propertyDAO = propertyDAO;
        this.priceRecordDAO = priceRecordDAO;
        this.provinciaDao = provinciaDao;
        this.ayuntamientoDao = ayuntamientoDao;
        this.sessionFactory = sessionFactory;
    }

    public boolean collectPropertiesForProvinceName(String provinceName, Set<IdealistaPropertyType> propertyTypes) {
        Set<String> ayuntamientosForProvince = getAyuntamientosForProvince(provinceName)
                .stream()
                .map(Ayuntamiento::getName)
                .collect(Collectors.toSet());

        Set<IdealistaSearchRequest> searchRequests = new HashSet<>();
        propertyTypes.forEach(type -> {
            Set<IdealistaSearchRequest> typeRequests = ayuntamientosForProvince
                    .stream()
                    .map(ayuntamiento -> IdealistaSearchRequest.saleSearch(ayuntamiento, type))
                    .collect(Collectors.toSet());
            searchRequests.addAll(typeRequests);
        });

        return startPropertyCollectionForProvince(searchRequests);
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

    private boolean startPropertyCollectionForProvince(Set<IdealistaSearchRequest> searchRequests) {
        LOGGER.info("Triggering {} search requests", searchRequests.size());
        Set<ApifyCollector.ApifyPendingSearch> pendingSearches = triggerSearches(searchRequests);
        LOGGER.info("Finished {} search requests", pendingSearches.size());

        LOGGER.info("Sending {} status requests", pendingSearches.size());
        Set<ApifyCollector.ApifyPendingSearch> finishedSearches = awaitSearchesFinishing(pendingSearches);
        LOGGER.info("Confirmed {} requests have finished", finishedSearches.size());

        LOGGER.info("Storing completed searches");
        Set<IdealistaSearchRequest> requestsToBeFragmented = storeSearchResults(finishedSearches);

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

            startPropertyCollectionForProvince(fragmentedRequests);
        } else {
            LOGGER.info("No searches required fragmentation; collection complete");
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

    private Set<ApifyCollector.ApifyPendingSearch> triggerSearches(Set<IdealistaSearchRequest> toRun) {
        return executeIteratively(APIFY_ACTIVE_AGENTS, toRun, (IdealistaSearchRequest searchRequest) -> {
            PendingSearchOrError result = collector.startSearch(searchRequest);
            if (!result.isSucceeded()) {
                LOGGER.debug("Request failed with error {}", result.getError().get());
                return null;
            }
            return result.getPendingSearch().get();
        });
    }

    private Set<ApifyCollector.ApifyPendingSearch> awaitSearchesFinishing(Set<ApifyCollector.ApifyPendingSearch> pendingSearches) {
        return executeIteratively(APIFY_ACTIVE_AGENTS, pendingSearches, (ApifyCollector.ApifyPendingSearch pendingSearch) -> {
            if (collector.getSearchStatus(pendingSearch) == ApifyCollector.PENDING_SEARCH_STATUS.SUCCEEDED) {
                return pendingSearch;
            }
            return null;
        });
    }

    // This has to be single threaded to avoid deadlocks from storing identical properties from different batches.
    private Set<IdealistaSearchRequest> storeSearchResults(Set<ApifyCollector.ApifyPendingSearch> finishedSearches) {
        Set<IdealistaSearchRequest> requestsToFragment = ConcurrentHashMap.newKeySet();

        // Process sequentially to avoid deadlocks
        for (ApifyCollector.ApifyPendingSearch search : finishedSearches) {
            try {
                HibernateUtils.executeRunnableInSessionWithTransaction(sessionFactory, () -> {
                    Set<IdealistaProperty> properties = collector.getSearchResults(search);

                    if (properties.size() > 2000) {
                        requestsToFragment.add(search.request());
                    }

                    properties.forEach(this::processProperty);
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
        return HibernateUtils.executeUntilAllSuccessful(toRun, resultOrNullFunction, batchSize, executorService);
    }

    private void processProperty(IdealistaProperty property) {
        IdealistaContactInformation definitiveContactInformation = establishContactInformation(property.getContactInfo());
        PropertyResult propertyResult = establishProperty(property);
        IdealistaProperty definitiveIdealistaProperty = propertyResult.idealistaProperty;

        property.setContactInfo(definitiveContactInformation);
        propertyDAO.saveOrUpdate(definitiveIdealistaProperty);
    }

    private IdealistaContactInformation establishContactInformation(IdealistaContactInformation newContactInformation) {
        IdealistaContactInformation definitiveContactInformation;

        if (newContactInformation.getPhoneNumber() == 0) {
            definitiveContactInformation = contactDAO.create(newContactInformation);
        } else {
            definitiveContactInformation = contactDAO
                    .findByPhoneNumber(newContactInformation.getPhoneNumber())
                    .orElse(contactDAO.create(newContactInformation));
        }
        return definitiveContactInformation;
    }

    private PropertyResult establishProperty(IdealistaProperty newProperty) {
        final long propertyCode = newProperty.getPropertyCode();
        Optional<IdealistaProperty> maybeProperty = propertyDAO.findByPropertyCode(propertyCode);

        if (maybeProperty.isPresent()) {
            final long currentTimestamp = Instant.now().toEpochMilli();
            IdealistaProperty existingProperty = maybeProperty.get();
            existingProperty.setLastSeen(currentTimestamp);

            boolean priceHasChanged = false;
            Optional<IdealistaPriceRecord> maybePriceRecord = this.priceRecordDAO.findLatestByPropertyCode(propertyCode);
            if (maybePriceRecord.isEmpty()) {
                LOGGER.error("Found existing property but no existing price record {}", existingProperty);
            } else {
                final long newPrice = newProperty.getPriceRecords().get(0).getPrice();
                if (maybePriceRecord.get().getPrice() != newPrice) {
                    LOGGER.debug("Price has changed! {}", propertyCode);
                    priceHasChanged = true;
                    IdealistaPriceRecord priceRecord = new IdealistaPriceRecord(
                            propertyCode, newPrice, currentTimestamp);
                    existingProperty.getPriceRecords().add(priceRecord);
                }
            }
            return new PropertyResult(existingProperty, priceHasChanged
                    ? PropertyResult.Result.PRICE_CHANGE
                    : PropertyResult.Result.EXISTING_IDENTICAL);
        }
        return new PropertyResult(newProperty, PropertyResult.Result.NEW);
    }

    private record PropertyResult(IdealistaProperty idealistaProperty, Result result) {
        public enum Result {
            EXISTING_IDENTICAL,
            PRICE_CHANGE,
            NEW
        }
    }
}
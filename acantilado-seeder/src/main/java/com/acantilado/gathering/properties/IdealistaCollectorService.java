package com.acantilado.gathering.properties;


import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDao;
import com.acantilado.core.administrative.Provincia;
import com.acantilado.core.administrative.ProvinciaDao;
import com.acantilado.core.properties.idealista.*;
import com.acantilado.gathering.properties.collectors.ApifyCollector;
import com.acantilado.gathering.properties.collectors.IdealistaCollector;
import com.acantilado.gathering.properties.collectors.PendingSearchOrError;
import com.acantilado.gathering.properties.idealistaTypes.IdealistaOperation;
import com.acantilado.gathering.properties.idealistaTypes.IdealistaPropertyType;
import com.acantilado.gathering.properties.idealistaTypes.IdealistaSearchRequest;
import com.acantilado.gathering.properties.queries.DefaultIdealistaSearchQueries.IdealistaSearch;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public final class IdealistaCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollectorService.class);

    private final IdealistaContactInformationDAO contactDAO;
    private final IdealistaPropertyDAO propertyDAO;
    private final IdealistaPriceRecordDAO priceRecordDAO;
    private final ProvinciaDao provinciaDao;
    private final AyuntamientoDao ayuntamientoDao;
    private final SessionFactory sessionFactory;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final IdealistaCollector collector = new IdealistaCollector();

    public IdealistaCollectorService(IdealistaContactInformationDAO contactDAO,
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

    public void collectPropertiesForProvinceName(String provinceName) {
        Set<IdealistaSearchRequest> landSearchRequests = getAyuntamientosForProvince(provinceName)
                .stream()
                .map(Ayuntamiento::getName)
                .map(IdealistaCollectorService::landSaleSearch)
                .collect(Collectors.toUnmodifiableSet());

        LOGGER.info("About to trigger {} requests for province {}", landSearchRequests.size(), provinceName);
        Set<ApifyCollector.ApifyPendingSearch> pendingSearches = triggerSearches(landSearchRequests);
        LOGGER.info("Finished {} requests for province {}", pendingSearches.size(), provinceName);

        Queue<ApifyCollector.ApifyPendingSearch> pendingQueue = new ConcurrentLinkedQueue<>(pendingSearches);
//
//        while (!pendingQueue.isEmpty()) {
//            int batchSize = pendingQueue.size();
//            LOGGER.info("Processing batch of {} searches", batchSize);
//
//            List<CompletableFuture<Void>> futures = new ArrayList<>();
//
//            for (int i = 0; i < batchSize; i++) {
//                ApifyCollector.ApifyPendingSearch search = pendingQueue.poll();
//                if (search == null) break;
//
//                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                    try {
//                        if (searchHasFinished(search)) {
//                            LOGGER.info("Search has finished {}", search);
//                            executeRunnableInSessionWithTransaction(() -> {
//                                processSearchResults(search);
//                            });
//                        } else {
//                            LOGGER.info("Search has not finished yet {}", search);
//                            pendingQueue.add(search);
//                        }
//                    } catch (Exception e) {
//                        LOGGER.error("Error processing search {}", search, e);
//                    }
//                }, executorService);
//                futures.add(future);
//            }
//
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//            // Always delay between batches to avoid hammering the service
//            if (!pendingQueue.isEmpty()) {
//                try {
//                    LOGGER.info("Waiting before next batch, {} searches remaining", pendingQueue.size());
//                    Thread.sleep(10000);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    throw new RuntimeException(e);
//                }
//            }
//        }

        LOGGER.info("Finished processing all searches for province {}", provinceName);
    }

    private List<Ayuntamiento> getAyuntamientosForProvince(String provinceName) {
        return executeCallableInSessionWithoutTransaction(() -> {
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
        Queue<IdealistaSearchRequest> requestsToRun = new ConcurrentLinkedQueue<>(toRun);
        Set<ApifyCollector.ApifyPendingSearch> requestsThatSucceeded = ConcurrentHashMap.newKeySet();

        while (!requestsToRun.isEmpty()) {
            Set<IdealistaSearchRequest> nextBatch = new HashSet<>();
            for (int i = 0; i < 32 && !requestsToRun.isEmpty(); i++) {
                nextBatch.add(requestsToRun.poll());
            }

            List<CompletableFuture<ApifyCollector.ApifyPendingSearch>> futures = nextBatch
                    .stream()
                    .map(request ->
                            CompletableFuture.supplyAsync(() -> executeCallableInSessionWithoutTransaction(() -> {
                                PendingSearchOrError result = collector.startSearch(request);
                                if (!result.isSucceeded()) {
                                    requestsToRun.add(request);
                                    LOGGER.debug("Request failed with error {}", result.getError().get());
                                    return null;
                                }

                                return result.getPendingSearch().get();
                            }), executorService))
                    .toList();

            Set<ApifyCollector.ApifyPendingSearch> successfulStarts = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .peek(requestsThatSucceeded::add)
                    .collect(Collectors.toSet());
            requestsThatSucceeded.addAll(successfulStarts);

            LOGGER.info("Successfully triggered {} requests, {} this run, {} remain",
                    requestsThatSucceeded.size(),
                    successfulStarts.size(),
                    requestsToRun.size());

            try {
                Thread.sleep(5000);
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        }

        return requestsThatSucceeded;
    }

    private boolean searchHasFinished(ApifyCollector.ApifyPendingSearch pendingSearch) {
        return executeCallableInSessionWithoutTransaction(() ->
                collector.getSearchStatus(pendingSearch) == ApifyCollector.PENDING_SEARCH_STATUS.SUCCEEDED);
    }

    private void processSearchResults(ApifyCollector.ApifyPendingSearch finishedSearch) {
        collector.getSearchResults(finishedSearch).forEach(this::processProperty);
        LOGGER.info("Successfully completed search for request {}", finishedSearch);
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
                    LOGGER.info("Price has changed! {}", propertyCode);
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

    private static IdealistaSearchRequest landSaleSearch(String location) {
        return IdealistaSearchRequest.fromSearch(
                new IdealistaSearch(IdealistaOperation.SALE, IdealistaPropertyType.LAND, location));
    }

    private static IdealistaSearchRequest homeSaleSearch(String location) {
        return IdealistaSearchRequest.fromSearch(
                new IdealistaSearch(IdealistaOperation.SALE, IdealistaPropertyType.HOME, location));
    }

    private static class PropertyResult {
        private final IdealistaProperty idealistaProperty;
        private final Result result;

        public enum Result {
            EXISTING_IDENTICAL,
            PRICE_CHANGE,
            NEW
        }

        public PropertyResult(IdealistaProperty idealistaProperty, Result result) {
            this.idealistaProperty = idealistaProperty;
            this.result = result;
        }
    }

    private <T> T executeCallableInSessionWithoutTransaction(Callable<T> callable) {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);

        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    private void executeRunnableInSessionWithoutTransaction(Runnable runnable) {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);

        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    private void executeRunnableInSessionWithTransaction(Runnable runnable) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        ManagedSessionContext.bind(session);

        try {
            runnable.run();
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }
}
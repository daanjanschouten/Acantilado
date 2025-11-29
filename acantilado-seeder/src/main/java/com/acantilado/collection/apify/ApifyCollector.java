package com.acantilado.collection.apify;

import com.acantilado.collection.Collector;
import com.acantilado.collection.utils.RequestBodyData;
import com.acantilado.utils.RetryableBatchedExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ApifyCollector<S extends RequestBodyData, T> extends Collector<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApifyCollector.class);

    private static final String AUTHORITY = "api.apify.com";
    private static final String DELIMITER = "/";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String RUN_FIELD = "id";
    private static final String STATUS_FIELD = "status";
    private static final String DATASET_FIELD = "defaultDatasetId";
    private static final String DATA_FIELD = "data";
    private static final String ACTS_PATH = "/v2/acts";
    private static final String DATASETS_PATH = "/v2/datasets";
    private static final String ITEMS = "items";

    private static final String AUTH_HEADER = "";
    private static final Optional<Integer> APIFY_ACTIVE_AGENTS = Optional.of(32);

    protected final String getActorId() {
        return "REcGj6dyoIJ9Z7aE6";
    }
    private final ExecutorService executorService;
    private final SessionFactory sessionFactory;

    public abstract void storeResult(T result);

    public ApifyCollector(ExecutorService executorService, SessionFactory sessionFactory) {
        super(AUTHORITY);

        this.executorService = executorService;
        this.sessionFactory = sessionFactory;
    }

    public ApifySearchResults<S> startCollection(Set<S> requests) {
        LOGGER.info("Triggering {} search requests", requests.size());
        Set<ApifyRunningSearch<S>> pendingSearches = triggerSearches(requests);
        LOGGER.info("Submitted {} search requests", requests.size());

        Set<ApifyRunningSearch<S>> finishedSearches = awaitSearchesFinishing(pendingSearches);
        LOGGER.info("Confirmed {} searches have finished", finishedSearches.size());

        ApifySearchResults<S> results = storeSearchResults(finishedSearches);
        LOGGER.info("Search results: {}", results);
        return results;
    }

    private Set<ApifyRunningSearch<S>> triggerSearches(Set<S> toRun) {
        return RetryableBatchedExecutor.executeUntilAllSuccessful(
                toRun,
                APIFY_ACTIVE_AGENTS,
                executorService,
                this::startSearch
        );
    }

    private Set<ApifyRunningSearch<S>> awaitSearchesFinishing(Set<ApifyRunningSearch<S>> pendingSearches) {
        return RetryableBatchedExecutor.executeUntilAllSuccessful(
                pendingSearches,
                APIFY_ACTIVE_AGENTS,
                executorService,
                (ApifyRunningSearch<S> pendingSearch) -> {
                    ApifySearchStatus searchStatus = this.getSearchStatus(pendingSearch);
                    if (searchStatus.hasFinished()) {
                        return pendingSearch.withStatus(searchStatus);
                    }
                    return null;
                });
    }

    private ApifySearchResults<S> storeSearchResults(Set<ApifyRunningSearch<S>> finishedSearches) {
        try {
            return RetryableBatchedExecutor.executeCallableInSessionWithTransaction(sessionFactory,
                    () -> this.storeResults(finishedSearches));
        } catch (Exception e) {
            LOGGER.error("Failed to store results for search: {}", finishedSearches, e);
            return new ApifySearchResults<>(Set.of(), Set.of(), Set.of(), Set.of());
        }
    }

    @Override
    protected Iterator<Collection<T>> seed() {
        throw new RuntimeException("Unsupported");
    }

    public ApifyRunningSearch<S> startSearch(S request) {
        JsonNode requestStarted = makePostHttpRequest(
                constructActsUri(""), request.toRequestBodyString(), AUTH_HEADER);

        if (Objects.isNull(requestStarted.get(DATA_FIELD))) {
            String error = requestStarted.get(ERROR).get(MESSAGE).textValue();
            LOGGER.debug("Request start failed with error {}", error);
            return null;
        }

        return new ApifyRunningSearch<>(
                request,
                ApifySearchStatus.TO_BE_SUBMITTED,
                requestStarted.get(DATA_FIELD).get(RUN_FIELD).textValue(),
                requestStarted.get(DATA_FIELD).get(DATASET_FIELD).textValue());
    }

    public ApifySearchStatus getSearchStatus(ApifyRunningSearch runningSearch) {
        JsonNode requestStatus = makeGetHttpRequest(constructActsUri(runningSearch.runId()), AUTH_HEADER);
        String status = requestStatus.get(DATA_FIELD).get(STATUS_FIELD).textValue();

        return ApifySearchStatus.valueOf(status.replace("-", "_"));
    }

    public ApifySearchResults<S> storeResults(Set<ApifyRunningSearch<S>> finishedSearches) {
        Set<S> requestsSucceeded = ConcurrentHashMap.newKeySet();
        Set<S> requestsToFragment = ConcurrentHashMap.newKeySet();
        Set<S> requestsToRetryDueToProxy = ConcurrentHashMap.newKeySet();
        Set<S> requestsToRetryDueToFailure = ConcurrentHashMap.newKeySet();

        LOGGER.debug("Processing search results for {} finished searches", finishedSearches.size());

        finishedSearches.forEach(search -> {
            if (search.pendingSearchStatus().hasFailed()) {
                requestsToRetryDueToFailure.add(search.request());
                return;
            }

            Iterator<JsonNode> iterator = getSuccessfulSearchResults(search);
            AtomicInteger objectsProcessed = new AtomicInteger();
            Set<T> jsonObjects = new HashSet<>();

            while (iterator.hasNext()) {
                T translatedObject = constructObject(iterator.next());
                objectsProcessed.incrementAndGet();
                jsonObjects.add(translatedObject);
            }

            if (objectsProcessed.get() > 2300) {
                requestsToFragment.add(search.request());
            }

            if (jsonObjects.isEmpty()) {
                requestsToRetryDueToProxy.add(search.request());
                LOGGER.debug("No results, will be retried with residential proxy: {}", search.request());
                return;
            }

            jsonObjects.forEach(this::storeResult);
            requestsSucceeded.add(search.request());

            LOGGER.debug("Stored batch of {} results", jsonObjects.size());
        });

        return new ApifySearchResults(requestsSucceeded, requestsToFragment, requestsToRetryDueToFailure, requestsToRetryDueToProxy);
    }

    private Iterator<JsonNode> getSuccessfulSearchResults(ApifyRunningSearch finishedSearch) {
        URI uri = constructDatasetsUri(finishedSearch.datasetId());
        JsonNode node = makeGetHttpRequest(uri, AUTH_HEADER);
        return node.elements();
    }

    private URI constructActsUri(String extension) {
        String path = StringUtils.joinWith(DELIMITER, ACTS_PATH, getActorId(), "runs", extension);
        return constructUri(path);
    }

    private URI constructDatasetsUri(String datasetId) {
        String path = StringUtils.joinWith(DELIMITER, DATASETS_PATH, datasetId, ITEMS);
        return constructUri(path);
    }
}

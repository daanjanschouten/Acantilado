package com.acantilado.gathering.properties.collectors;

import com.acantilado.gathering.Collector;
import com.acantilado.gathering.properties.idealista.IdealistaSearchRequest;
import com.acantilado.gathering.properties.apify.ApifyRunningSearch;
import com.acantilado.gathering.properties.apify.ApifySearchStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

public abstract class ApifyCollector<T> extends Collector<T> {
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

    // Fix Double Ayuntamiento

    protected abstract String getActorId();

    public ApifyCollector() {
        super(AUTHORITY);
    }

    @Override
    protected Iterator<Collection<T>> seed() {
        throw new RuntimeException("Unsupported");
    }

    public ApifyRunningSearch startSearch(IdealistaSearchRequest request) {
        JsonNode requestStarted = makePostHttpRequest(
                constructActsUri(""), request.toRequestBodyString(), AUTH_HEADER);

        if (Objects.isNull(requestStarted.get(DATA_FIELD))) {
            String error = requestStarted.get(ERROR).get(MESSAGE).textValue();
            LOGGER.debug("Request start failed with error {}", error);
            return null;
        }

        return new ApifyRunningSearch(
                request,
                ApifySearchStatus.TO_BE_SUBMITTED,
                requestStarted.get(DATA_FIELD).get(RUN_FIELD).textValue(),
                requestStarted.get(DATA_FIELD).get(DATASET_FIELD).textValue());
    }

    public ApifySearchStatus getSearchStatus(ApifyRunningSearch runDetails) {
        JsonNode requestStatus = makeGetHttpRequest(constructActsUri(runDetails.runId()), AUTH_HEADER);
        String status = requestStatus.get(DATA_FIELD).get(STATUS_FIELD).textValue();
        return ApifySearchStatus.valueOf(status);
    }

    public Set<T> getSearchResults(ApifyRunningSearch runDetails) {
        Set<T> apifyObjects = new HashSet<>();
        JsonNode node = makeGetHttpRequest(constructDatasetsUri(runDetails.datasetId()), AUTH_HEADER);

        Iterator<JsonNode> individualObjects = node.elements();

        while (individualObjects.hasNext()) {
            T translatedObject = constructObject(individualObjects.next());
            apifyObjects.add(translatedObject);
        }

        return apifyObjects;
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

package com.acantilado.collection.properties.collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.acantilado.collection.Collector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.*;

public abstract class ApifyCollector<T> extends Collector<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApifyCollector.class);

    private static final String AUTHORITY = "api.apify.com";
    private static final String DELIMITER = "/";
    private static final String RUN_FIELD = "id";
    private static final String STATUS_FIELD = "status";
    private static final String DATASET_FIELD = "defaultDatasetId";
    private static final String DATA_FIELD = "data";
    private static final String ACTS_PATH = "/v2/acts";
    private static final String DATASETS_PATH = "/v2/datasets";
    private static final String ITEMS = "items";

    private static final String AUTH_HEADER = "";

    public enum PENDING_SEARCH_STATUS {
        STARTED,
        RUNNING,
        SUCCEEDED
    }

    public record ApifyPendingSearch(String runId, String datasetId) {}

    protected abstract String getActorId();

    public ApifyCollector() {
        super(AUTHORITY);
    }

    @Override
    protected Iterator<Collection<T>> seed() {
        throw new RuntimeException("Unsupported");
    }

    public ApifyPendingSearch startSearch(HttpRequest.BodyPublisher body) {
        JsonNode requestStarted = makePostHttpRequest(constructActsUri(""), body, AUTH_HEADER);

        return new ApifyPendingSearch(
                requestStarted.get(DATA_FIELD).get(RUN_FIELD).textValue(),
                requestStarted.get(DATA_FIELD).get(DATASET_FIELD).textValue());
    }

    public PENDING_SEARCH_STATUS getSearchStatus(ApifyPendingSearch runDetails) {
        JsonNode requestStatus = makeGetHttpRequest(constructActsUri(runDetails.runId()), AUTH_HEADER);
        String status = requestStatus.get(DATA_FIELD).get(STATUS_FIELD).textValue();
        return PENDING_SEARCH_STATUS.valueOf(status);
    }

    public Set<T> getSearchResults(ApifyPendingSearch runDetails) {
        Set<T> apifyObjects = new HashSet<>();
        JsonNode node = makeGetHttpRequest(constructDatasetsUri(runDetails.datasetId()), AUTH_HEADER);

        Iterator<JsonNode> individualObjects = node.elements();

        while (individualObjects.hasNext()) {
            Optional<T> translatedObject = constructObject(individualObjects.next());
            translatedObject.ifPresent(apifyObjects::add);
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

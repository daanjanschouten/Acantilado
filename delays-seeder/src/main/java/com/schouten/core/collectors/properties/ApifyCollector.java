package com.schouten.core.collectors.properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.schouten.core.collectors.Collector;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.*;

public abstract class ApifyCollector<T> extends Collector<T> {
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

    public record ApifyRunDetails (String runId, String datasetId) {}

    protected abstract String getActorId();

    public ApifyCollector() {
        super(AUTHORITY);
    }

    @Override
    protected Iterator<Collection<T>> seed() {
        throw new RuntimeException("Unsupported");
    }

    public ApifyRunDetails startSearch(HttpRequest.BodyPublisher body) {
        JsonNode requestStarted = makePostHttpRequest(constructActsUri(""), body, AUTH_HEADER);

        return new ApifyRunDetails(
                requestStarted.get(DATA_FIELD).get(RUN_FIELD).textValue(),
                requestStarted.get(DATA_FIELD).get(DATASET_FIELD).textValue());
    }

    public String getSearchStatus(ApifyRunDetails runDetails) {
        JsonNode requestStatus = makeGetHttpRequest(constructActsUri(runDetails.runId()), AUTH_HEADER);
        return requestStatus.get(DATA_FIELD).get(STATUS_FIELD).textValue();
    }

    public Set<T> getSearchResults(ApifyRunDetails runDetails) {
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

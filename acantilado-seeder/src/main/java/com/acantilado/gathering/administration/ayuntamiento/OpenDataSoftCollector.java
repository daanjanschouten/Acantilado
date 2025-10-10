package com.acantilado.gathering.administration.ayuntamiento;

import com.fasterxml.jackson.databind.JsonNode;
import com.acantilado.gathering.Collector;
import com.acantilado.gathering.CollectorIterator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public abstract class OpenDataSoftCollector<T> extends Collector<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataSoftCollector.class);

    private static final String AUTHORITY = "public.opendatasoft.com";
    private static final String PATH = "/api/explore/v2.1/catalog/datasets";
    private static final String RECORDS = "records";
    private static final String RESULTS = "results";

    private static final String LIMIT_PARAM = "limit";
    private static final String OFFSET_PARAM = "offset";
    private static final int API_BATCH_LIMIT = 100;

    private final String path;

    public OpenDataSoftCollector(String datasetName) {
        super(AUTHORITY);

        this.path = StringUtils.joinWith("/", PATH, datasetName, RECORDS);
    }

    @Override
    public final Iterator<Collection<T>> seed() {
        return new CollectorIterator<>(
                this::fetchRecords,
                API_BATCH_LIMIT,
                (Optional<URI> maybeUri) -> this.constructUri(path, maybeUri, this::buildQuery));
    }

    private Set<T> fetchRecords(URI uri) {
        Set<T> openDataSoftObjects = new HashSet<>();

        JsonNode node = makeGetHttpRequest(uri).get(RESULTS);
        LOGGER.debug("Found {} records", node.size());

        Iterator<JsonNode> individualObjects = node.elements();
        while (individualObjects.hasNext()) {
            Optional<T> translatedObject = constructObject(individualObjects.next());
            translatedObject.ifPresent(openDataSoftObjects::add);
        }

        return openDataSoftObjects;
    }

    private String buildQuery(Optional<URI> existingUri) {
        if (existingUri.isPresent()) {
            return increaseQueryOffset(existingUri.get().getQuery());
        } else {
            String maxRows = StringUtils.join(LIMIT_PARAM, "=", API_BATCH_LIMIT);
            String offset = StringUtils.join(OFFSET_PARAM, "=", 0);
            return StringUtils.joinWith("&", maxRows, offset);
        }
    }

    private static String increaseQueryOffset(String existingQuery) {
        Map<String, String> params = new HashMap<>();
        for (String param : existingQuery.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                params.put(pair[0], pair[1]);
            }
        }

        int currentOffset = Integer.parseInt(params.get(OFFSET_PARAM));
        params.put(OFFSET_PARAM, String.valueOf(currentOffset + API_BATCH_LIMIT));

        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}

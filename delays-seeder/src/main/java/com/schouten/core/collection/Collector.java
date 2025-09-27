package com.schouten.core.collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.schouten.core.collection.utils.HttpUtils;
import io.dropwizard.hibernate.UnitOfWork;
import org.eclipse.jetty.http.HttpScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

public abstract class Collector<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Collector.class);

    private final String authority;

    public Collector(String authority) {
        this.authority = authority;
    }

    protected abstract Iterator<Collection<T>> seed();

    @UnitOfWork
    protected abstract Optional<T> constructObject(JsonNode jsonNode);

    protected final JsonNode makePostHttpRequest(URI uri, HttpRequest.BodyPublisher body, String authorizationHeader)  {
        LOGGER.debug("Constructing HTTP POST request for URI: {} with body: {}", uri, body);

        HttpRequest request = HttpRequest.newBuilder()
                .POST(body)
                .header("Authorization", authorizationHeader)
                .header("Content-Type", "application/json")
                .uri(uri)
                .build();

        return HttpUtils.makeApiCall(request);
    }

    protected final JsonNode makeGetHttpRequest(URI uri) {
        return makeGetHttpRequest(uri, "");
    }

    protected final JsonNode makeGetHttpRequest(URI uri, String authorizationHeader) {
        LOGGER.debug("Constructing HTTP GET request for URI: {}", uri);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", authorizationHeader)
                .header("Content-Type", "application/json")
                .uri(uri)
                .build();

        return HttpUtils.makeApiCall(request);
    }

    protected final URI constructUri(String path) {
        return constructUri(path, Optional.empty(), _ignored -> "");
    }

    protected final URI constructUri(String path, Optional<URI> maybeExistingUri, Function<Optional<URI>, String> queryTransformer) {
        try {
            return new URI(
                    HttpScheme.HTTPS.toString(),
                    authority,
                    path,
                    queryTransformer.apply(maybeExistingUri),
                    null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
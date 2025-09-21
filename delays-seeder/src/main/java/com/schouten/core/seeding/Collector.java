package com.schouten.core.seeding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.http.HttpScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public abstract class Collector<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Collector.class);

    protected abstract String getAuthority();

    protected abstract String getPath();

    protected abstract String getQuery(Optional<URI> existingUri);

    protected abstract Iterator<Collection<T>> seed();

    protected JsonNode makeApiGetCall(URI uri) {
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(
                    constructGetHttpRequest(uri),
                    HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        try (InputStream inputStream = response.body()) {
            return new ObjectMapper().readTree(inputStream);
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to read data returned by external source", ioException);
        }
    }

    private static HttpRequest constructGetHttpRequest(URI uri) {
        LOGGER.info("Constructing HTTP request for URI: {}", uri);
        return HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();
    }

    protected URI constructUri(Optional<URI> maybeExistingUri) {
        try {
            return new URI(
                    HttpScheme.HTTPS.toString(),
                    getAuthority(),
                    getPath(),
                    getQuery(maybeExistingUri),
                    null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
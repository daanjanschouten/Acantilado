package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.schouten.core.ApiConstants;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public abstract class FlightLabsSeeder<T> {
    String API_AIRLINE_IATA_ID = "codeIataAirline";
    String API_COUNTRY_ISO = "codeIso2Country";
    String API_DATA = "data";

    abstract String getApiPrefix();

    abstract Optional<T> constructObject(JsonNode jsonNode);

    Set<String> getAdditionalParams() {
        return Set.of("");
    }

    public final Set<T> seed() {
        Set<T> flightLabsObjects = new HashSet<>();
        this.getAdditionalParams().forEach(param -> {
            try {
                String uriString = constructUriString(getApiPrefix(), param);
                JsonNode node = makeApiCall(URI.create(uriString)).get(API_DATA);
                Iterator<JsonNode> individualObjects = node.elements();
                while (individualObjects.hasNext()) {
                    Optional<T> flightLabsObject = constructObject(individualObjects.next());
                    flightLabsObject.ifPresent(flightLabsObjects::add);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return flightLabsObjects;
    }

    @VisibleForTesting
    String constructUriString(String prefix, String additionalParam) {
        return StringUtils.join(
                ApiConstants.getApiBaseUrl(),
                prefix,
                ApiConstants.getApiKeyPair(),
                additionalParam);
    }

    @VisibleForTesting
    JsonNode makeApiCall(URI uri) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpResponse<InputStream> response = httpClient.send(
                constructGetHttpRequest(uri),
                HttpResponse.BodyHandlers.ofInputStream());;
        try (InputStream inputStream = response.body()) {
            return new ObjectMapper().readTree(inputStream);
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to read data returned by FlightLabs", ioException);
        }
    }

    private static HttpRequest constructGetHttpRequest(URI uri) {
        return HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();
    }
}

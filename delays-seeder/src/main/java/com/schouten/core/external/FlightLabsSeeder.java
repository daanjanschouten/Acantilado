package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public interface FlightLabsSeeder<T> {
    String API_AIRLINE_IATA_ID = "codeIataAirline";
    String API_COUNTRY_ISO = "codeIso2Country";
    String getApiPrefix();

    default Set<String> getAdditionalParams() {
        return Set.of("");
    }

    Optional<T> constructObject(JsonNode jsonNode);

    default Set<T> seed() {
        Set<T> flightLabsObjects = new HashSet<>();
        this.getAdditionalParams().forEach(p -> {
            try {
                JsonNode node = makeApiCall(p);
                Iterator<JsonNode> elements = node.elements();
                while (elements.hasNext()) {
                    JsonNode flightLabsObjectsJson = elements.next();
                    Optional<T> flightLabsObject = constructObject(flightLabsObjectsJson);
                    flightLabsObject.ifPresent(flightLabsObjects::add);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return flightLabsObjects;
    }

    private JsonNode makeApiCall(String additionalParam) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpResponse<InputStream> response = client.send(
                buildRequest(additionalParam),
                HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream inputStream = response.body()) {
            return new ObjectMapper().readTree(inputStream).get("data");
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to read data returned by FlightLabs", ioException);
        }
    }

    private HttpRequest buildRequest(String additionalParam) {
        String uriString = StringUtils.join(
                ApiConstants.getApiBaseUrl(),
                getApiPrefix(),
                ApiConstants.getApiKeyPair(),
                additionalParam);
        return HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uriString))
                .build();
    }

}

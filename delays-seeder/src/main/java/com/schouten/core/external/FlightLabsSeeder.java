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
    String getParam();

    Optional<T> constructObject(JsonNode jsonNode);

    default Set<T> seed() throws IOException, InterruptedException {
        return constructObjects();
    }

    private Set<T> constructObjects() throws IOException, InterruptedException {
        Set<T> flightLabsObjects = new HashSet<>();
        Iterator<JsonNode> elements = makeApiCall().elements();
        while (elements.hasNext()) {
            JsonNode flightLabsObjectsJson = elements.next();
            Optional<T> flightLabsObject = constructObject(flightLabsObjectsJson);
            if (flightLabsObject.isPresent()) {
                flightLabsObjects.add(flightLabsObject.get());
            }
        }
        return flightLabsObjects;
    }

    private JsonNode makeApiCall() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpResponse<InputStream> response =
                client.send(this.buildRequest(), HttpResponse.BodyHandlers.ofInputStream());
        // InputStream inputStream = AirportSeeder.class.getClassLoader().getResourceAsStream("airports.json");
        try (InputStream inputStream = response.body()) {
            return new ObjectMapper().readTree(inputStream).get("data");
        }
    }

    private HttpRequest buildRequest() {
        String uriString = StringUtils.join(
                ApiConstants.getApiBaseUrl(),
                getParam(),
                ApiConstants.getApiKeyPair());
        return HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uriString))
                .build();
    }

}

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

    default Set<String> getAdditionalParams() {
        return Set.of("");
    }

    Optional<T> constructObject(JsonNode jsonNode);

    default Set<T> seed() throws IOException, InterruptedException {
        return constructObjects();
    }

    private Set<T> constructObjects() throws IOException, InterruptedException {
        Set<T> flightLabsObjects = new HashSet<>();
        makeApiCall().forEach(node -> {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                JsonNode flightLabsObjectsJson = elements.next();
                Optional<T> flightLabsObject = constructObject(flightLabsObjectsJson);
                flightLabsObject.ifPresent(flightLabsObjects::add);
            }
        });
        return flightLabsObjects;
    }

    private Set<JsonNode> makeApiCall() throws IOException, InterruptedException {
        Set<JsonNode> jsonNodes = new HashSet<>();
        HttpClient client = HttpClient.newBuilder().build();
        for (String p : getAdditionalParams()) {
            HttpRequest request = StringUtils.isEmpty(p)
                    ? buildRequest(p)
                    : buildRequest(StringUtils.join("&", p));
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream inputStream = response.body()) {
                JsonNode node = new ObjectMapper().readTree(inputStream).get("data");
                jsonNodes.add(node);
            }
        }
        return jsonNodes;
    }

    private HttpRequest buildRequest(String additionalParam) {
        String uriString = StringUtils.join(
                ApiConstants.getApiBaseUrl(),
                getParam(),
                ApiConstants.getApiKeyPair(),
                additionalParam);
        return HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uriString))
                .build();
    }

}

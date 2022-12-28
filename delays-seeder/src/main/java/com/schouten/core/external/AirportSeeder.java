package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schouten.core.ApiConstants;
import com.schouten.core.aviation.Airport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class AirportSeeder {
    private static final String airportParam = "airports";
    private static final Logger LOGGER = LoggerFactory.getLogger(AirportSeeder.class);

    public static Set<Airport> seedAirports() throws IOException, InterruptedException {
        return constructAirports(makeApiCall());
    }

    private static Set<Airport> constructAirports(JsonNode jsonNode) {
        Set<Airport> airports = new HashSet<>();
        Iterator<JsonNode> elements = jsonNode.elements();
        while (elements.hasNext()) {
            JsonNode airportsJson = elements.next();
            Airport airport = new Airport(
                    airportsJson.get("codeIataAirport").textValue(),
                    airportsJson.get("codeIso2Country").textValue(),
                    airportsJson.get("nameAirport").textValue(),
                    airportsJson.get("latitudeAirport").doubleValue(),
                    airportsJson.get("longitudeAirport").doubleValue());
            LOGGER.info(airport.toString());
            airports.add(airport);
        }
        return airports;
    }

    private static JsonNode makeApiCall() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpResponse<InputStream> response =
                client.send(buildAirportsRequest(), HttpResponse.BodyHandlers.ofInputStream());
        // InputStream inputStream = AirportSeeder.class.getClassLoader().getResourceAsStream("airports.json");
        try (InputStream inputStream = response.body()) {
            return new ObjectMapper().readTree(inputStream).get("data");
        }
    }

    private static HttpRequest buildAirportsRequest() {
        String uriString = StringUtils.join(
                ApiConstants.getApiBaseUrl(),
                airportParam,
                ApiConstants.getApiKeyPair());
        return HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uriString))
                .build();
    }
}

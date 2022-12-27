package com.schouten.core.external;

import com.schouten.core.ApiConstants;
import com.schouten.core.aviation.db.AirportDao;
import org.apache.commons.lang3.StringUtils;

import org.json.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class AirportSeeder {
    private final AirportDao airportDao;
    private final String airportParam = "airports";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AirportSeeder.class);

    public AirportSeeder(AirportDao airportDao) {
        this.airportDao = airportDao;
    }

    private String getAirportsUrl() {
        return StringUtils.join(ApiConstants.getApiBaseUrl(), airportParam, ApiConstants.getApiKeyPair());
    }

    public JSONObject makeApiCall() throws IOException, InterruptedException {
        LOGGER.info("Hello hello");
//        HttpClient client = HttpClient.newBuilder().build();
//        String uri = StringUtils.join(ApiConstants.getApiBaseUrl(), airportParam, ApiConstants.getApiKeyPair());
//        HttpRequest request = HttpRequest.newBuilder()
//                .GET()
//                .uri(URI.create(uri))
//                .build();
//
//        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
//        try (InputStream inputStream = response.body()) {
//            JSONObject json = new JSONObject(new JSONTokener(inputStream));
//            LOGGER.info(json.toString());
//            return json;
//        }
    }
}

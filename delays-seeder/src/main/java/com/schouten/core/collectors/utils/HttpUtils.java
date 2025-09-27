package com.schouten.core.collectors.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class HttpUtils {

    public static JsonNode makeApiCall(HttpRequest request) {
        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        try (InputStream inputStream = response.body()) {
            return new ObjectMapper().readTree(inputStream);
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to read data returned by external source", ioException);
        }
    }

    private HttpUtils() {}
}

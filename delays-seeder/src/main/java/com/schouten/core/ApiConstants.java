package com.schouten.core;

import org.apache.commons.lang3.StringUtils;

public class ApiConstants {
    private static final String API_BASE_URL = "https://app.goflightlabs.com/";
    private static final String API_KEY_PARAM = "access_key";
    private static final String API_KEY_VALUE = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiI0IiwianRpIjoiOTczNzM0YTc1NzY0NGVmZTdlZDNiNzc2YWNkNTQxZmE5OTc1NjNjOGNkYmU5MzNiNTVmMDU1MTM3MzkwM2JlZGMzMWFkZjBhMWMwMDFjY2IiLCJpYXQiOjE2NzE4OTA3OTAsIm5iZiI6MTY3MTg5MDc5MCwiZXhwIjoxNzAzNDI2NzkwLCJzdWIiOiIxOTM2MSIsInNjb3BlcyI6W119.wBKPPXRfVL9a_SnuIksMfc4-0sSxfGF5bT0knl8CZU183tFRypyZy3XvtZJjyv6M3rpRDMeurVsmwf4nj9-4ZA";

    public static String getApiBaseUrl() {
        return API_BASE_URL;
    }
    public static String getApiKeyPair() {
        return StringUtils.join("?", API_KEY_PARAM, "=", API_KEY_VALUE);
    }

    private ApiConstants() {
    }
}

package com.acantilado.collection.properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public class IdealistaCollectorConfig {
    @NotNull
    private Set<String> provinces = new HashSet<>(Set.of("Huelva", "Huesca"));

    @NotNull
    private Set<String> propertyTypes = new HashSet<>(Set.of("HOMES"));

    @NotNull
    private Duration initialDelay = Duration.seconds(10);

    @NotNull
    private Duration collectionInterval = Duration.days(1);

    @Min(1)
    private int schedulerThreadPoolSize = 1;

    @Min(1)
    private int collectorThreadPoolSize = 4;

    @NotNull
    private Duration shutdownTimeout = Duration.seconds(10);

    private boolean enabled = true;

    // API Configuration
    private String apiBaseUrl = "https://www.idealista.com";

    @Min(1)
    private int maxRetriesPerRequest = 3;

    @NotNull
    private Duration requestTimeout = Duration.seconds(30);

    @NotNull
    private Duration rateLimitDelay = Duration.milliseconds(500);

    @JsonProperty
    public Set<String> getProvinces() {
        return provinces;
    }

    @JsonProperty
    public void setProvinces(Set<String> provinces) {
        this.provinces = provinces;
    }

    @JsonProperty
    public Set<String> getPropertyTypes() {
        return propertyTypes;
    }

    @JsonProperty
    public void setPropertyTypes(Set<String> propertyTypes) {
        this.propertyTypes = propertyTypes;
    }

    @JsonProperty
    public Duration getInitialDelay() {
        return initialDelay;
    }

    @JsonProperty
    public void setInitialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
    }

    @JsonProperty
    public Duration getCollectionInterval() {
        return collectionInterval;
    }

    @JsonProperty
    public void setCollectionInterval(Duration collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    @JsonProperty
    public int getSchedulerThreadPoolSize() {
        return schedulerThreadPoolSize;
    }

    @JsonProperty
    public void setSchedulerThreadPoolSize(int schedulerThreadPoolSize) {
        this.schedulerThreadPoolSize = schedulerThreadPoolSize;
    }

    @JsonProperty
    public int getCollectorThreadPoolSize() {
        return collectorThreadPoolSize;
    }

    @JsonProperty
    public void setCollectorThreadPoolSize(int collectorThreadPoolSize) {
        this.collectorThreadPoolSize = collectorThreadPoolSize;
    }

    @JsonProperty
    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    @JsonProperty
    public void setShutdownTimeout(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    @JsonProperty
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JsonProperty
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    @JsonProperty
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    @JsonProperty
    public int getMaxRetriesPerRequest() {
        return maxRetriesPerRequest;
    }

    @JsonProperty
    public void setMaxRetriesPerRequest(int maxRetriesPerRequest) {
        this.maxRetriesPerRequest = maxRetriesPerRequest;
    }

    @JsonProperty
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    @JsonProperty
    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @JsonProperty
    public Duration getRateLimitDelay() {
        return rateLimitDelay;
    }

    @JsonProperty
    public void setRateLimitDelay(Duration rateLimitDelay) {
        this.rateLimitDelay = rateLimitDelay;
    }
}
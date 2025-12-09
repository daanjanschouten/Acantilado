package com.acantilado.collection.amenity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AmenityCollectorConfig {
    @NotNull
    private Set<String> provinces = new HashSet<>(Set.of());

    @NotNull
    private Set<String> amenityChains = new HashSet<>(Set.of("CARREFOUR"));

    @NotNull
    private Duration initialDelay = Duration.seconds(10);

    @NotNull
    private Duration collectionInterval = Duration.hours(6);

    @Min(1)
    private int schedulerThreadPoolSize = 1;

    @Min(1)
    private int collectorThreadPoolSize = 10;

    @NotNull
    private Duration shutdownTimeout = Duration.seconds(10);

    private boolean enabled = true;

    @JsonProperty
    public Set<String> getProvinces() {
        return provinces;
    }

    @JsonProperty
    public void setProvinces(Set<String> provinces) {
        this.provinces = provinces;
    }

    @JsonProperty
    public Set<String> getAmenityChains() {
        return amenityChains;
    }

    @JsonProperty
    public void setAmenityChains(Set<String> amenityChains) {
        this.amenityChains = amenityChains;
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
}
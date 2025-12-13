package com.acantilado.collection.amenity;

import com.acantilado.collection.CollectorConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AmenityCollectorConfig {
    @NotNull
    private Set<Integer> provinces = new HashSet<>(Set.of());

    @NotNull
    private Set<String> amenityChains = new HashSet<>(Set.of("CARREFOUR"));

    @NotNull
    private Duration collectionInterval = Duration.hours(6);

    @NotNull
    private Set<String> amenityTypes = Set.of();

    @Min(1)
    private int threadPoolSize = 10;

    private boolean enabled = true;

    @JsonProperty
    public Set<String> getProvinces() {
        return CollectorConfiguration.getProvinces(provinces);
    }

    @JsonProperty
    public void setProvinces(Set<Integer> provinces) {
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
        return CollectorConfiguration.getInitialDelay();
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
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    @JsonProperty
    public void setThreadPoolSize(int ThreadPoolSize) {
        this.threadPoolSize = ThreadPoolSize;
    }

    @JsonProperty
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getAmenityTypes() {
        return amenityTypes;
    }

    public void setAmenityTypes(Set<String> amenityTypes) {
        this.amenityTypes = amenityTypes;
    }
}
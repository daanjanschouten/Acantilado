package com.acantilado.collection.administration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


public class AdministrativeCollectorConfig {
    @NotNull
    private Duration initialDelay = Duration.seconds(5);

    @NotNull
    private Duration collectionInterval = Duration.days(7);

    @Min(1)
    private int threadPoolSize = 1;

    @NotNull
    private Duration shutdownTimeout = Duration.seconds(10);

    private boolean enabled = true;

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
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    @JsonProperty
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
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
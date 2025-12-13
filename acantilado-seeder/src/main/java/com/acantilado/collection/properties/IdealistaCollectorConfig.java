package com.acantilado.collection.properties;

import com.acantilado.collection.CollectorConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

public class IdealistaCollectorConfig {

  @NotNull private Set<Integer> provinces = new HashSet<>(Set.of());

  @NotNull private Set<String> propertyTypes = new HashSet<>(Set.of("HOMES"));

  @NotNull private Duration collectionInterval = Duration.days(1);

  @Min(1)
  private int threadPoolSize = 4;

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
  public Set<String> getPropertyTypes() {
    return propertyTypes;
  }

  @JsonProperty
  public void setPropertyTypes(Set<String> propertyTypes) {
    this.propertyTypes = propertyTypes;
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
  public void setThreadPoolSize(int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
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

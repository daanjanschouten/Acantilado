package com.acantilado.collection;

import com.acantilado.collection.administration.AdministrativeCollectorConfig;
import com.acantilado.collection.amenity.AmenityCollectorConfig;
import com.acantilado.collection.properties.IdealistaCollectorConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

public class CollectorConfiguration {
    public static Duration initialDelay = Duration.seconds(10);

    @Valid
    @NotNull
    private AmenityCollectorConfig amenityCollector = new AmenityCollectorConfig();

    @Valid
    @NotNull
    private AdministrativeCollectorConfig administrativeCollector = new AdministrativeCollectorConfig();

    @Valid
    @NotNull
    private IdealistaCollectorConfig idealistaCollector = new IdealistaCollectorConfig();

    @JsonProperty
    public static Duration getInitialDelay() {
        return initialDelay;
    }

    @JsonProperty
    public AmenityCollectorConfig getAmenityCollector() {
        return amenityCollector;
    }

    @JsonProperty
    public void setAmenityCollector(AmenityCollectorConfig amenityCollector) {
        this.amenityCollector = amenityCollector;
    }

    @JsonProperty
    public AdministrativeCollectorConfig getAdministrativeCollector() {
        return administrativeCollector;
    }

    @JsonProperty
    public void setAdministrativeCollector(AdministrativeCollectorConfig administrativeCollector) {
        this.administrativeCollector = administrativeCollector;
    }

    @JsonProperty
    public IdealistaCollectorConfig getIdealistaCollector() {
        return idealistaCollector;
    }

    @JsonProperty
    public void setIdealistaCollector(IdealistaCollectorConfig idealistaCollector) {
        this.idealistaCollector = idealistaCollector;
    }

    public static Set<String> getProvinces(Set<Integer> provinceIds) {
        return provinceIds
                .stream()
                .map(CollectorConfiguration::toStringWithMaybeLeadingZero)
                .collect(Collectors.toSet());
    }

    private static String toStringWithMaybeLeadingZero(int provinceId) {
        String stringValue = String.valueOf(provinceId);
        if (provinceId < 10) {
            return String.join("", String.valueOf(0), stringValue);
        }
        return stringValue;
    }
}

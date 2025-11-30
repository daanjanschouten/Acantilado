package com.acantilado;

import com.acantilado.collection.administration.AdministrativeCollectorConfig;
import com.acantilado.collection.amenity.AmenityCollectorConfig;
import com.acantilado.collection.properties.IdealistaCollectorConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public class AcantiladoConfiguration extends Configuration {
    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    private IdealistaCollectorConfig idealistaCollector = new IdealistaCollectorConfig();

    @Valid
    @NotNull
    private AmenityCollectorConfig amenityCollector = new AmenityCollectorConfig();

    @Valid
    @NotNull
    private AdministrativeCollectorConfig administrativeCollector = new AdministrativeCollectorConfig();

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        this.database = dataSourceFactory;
    }

    @JsonProperty("idealistaCollector")
    public IdealistaCollectorConfig getIdealistaCollector() {
        return idealistaCollector;
    }

    @JsonProperty("amenityCollector")
    public AmenityCollectorConfig getAmenityCollector() {
        return amenityCollector;
    }

    @JsonProperty("idealistaCollector")
    public void setIdealistaCollector(IdealistaCollectorConfig idealistaCollector) {
        this.idealistaCollector = idealistaCollector;
    }

    @JsonProperty("amenityCollector")
    public void setAmenityCollector(AmenityCollectorConfig amenityCollector) {
        this.amenityCollector = amenityCollector;
    }

    @JsonProperty("administrativeCollector")
    public AdministrativeCollectorConfig getAdministrativeCollector() {
        return administrativeCollector;
    }

    @JsonProperty("administrativeCollector")
    public void setAdministrativeCollector(AdministrativeCollectorConfig administrativeCollector) {
        this.administrativeCollector = administrativeCollector;
    }
}
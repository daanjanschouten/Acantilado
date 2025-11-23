package com.acantilado;

import com.acantilado.collection.administration.AdministrativeCollectorConfig;
import com.acantilado.collection.properties.IdealistaCollectorConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class AcantiladoConfiguration extends Configuration {
    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    private IdealistaCollectorConfig idealistaCollector = new IdealistaCollectorConfig();

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

    @JsonProperty("idealistaCollector")
    public void setIdealistaCollector(IdealistaCollectorConfig idealistaCollector) {
        this.idealistaCollector = idealistaCollector;
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
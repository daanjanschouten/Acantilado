package com.acantilado;

import com.acantilado.collection.CollectorConfiguration;
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
    @JsonProperty("collector")
    private CollectorConfiguration collectorConfiguration = new CollectorConfiguration();

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        this.database = dataSourceFactory;
    }

    public CollectorConfiguration getCollectorConfiguration() {
        return collectorConfiguration;
    }

    public void setCollectorConfiguration(CollectorConfiguration collectorConfiguration) {
        this.collectorConfiguration = collectorConfiguration;
    }
}
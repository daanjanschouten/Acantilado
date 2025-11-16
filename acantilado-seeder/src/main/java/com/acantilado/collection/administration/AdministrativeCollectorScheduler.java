package com.acantilado.collection.administration;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdministrativeCollectorScheduler implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdministrativeCollectorScheduler.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final GeographicCollectorService collectorService;

    public AdministrativeCollectorScheduler(GeographicCollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @Override
    public void start() {
        if (!collectorService.isSeedingNecessary()) {
            LOGGER.info("Skipping administrative collection because data already populated");
            return;
        }

        LOGGER.info("Starting administrative collection");
        scheduler.schedule(this::seed, 10, TimeUnit.SECONDS);
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping administrative collection");
        scheduler.shutdown();
        if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
        }
    }

    public void seed() {
        collectorService.seed();
        LOGGER.info("Finished administrative collection");
    }
}

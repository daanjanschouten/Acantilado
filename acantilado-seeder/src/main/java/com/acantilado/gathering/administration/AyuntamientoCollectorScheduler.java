package com.acantilado.gathering.administration;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AyuntamientoCollectorScheduler implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(AyuntamientoCollectorScheduler.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AyuntamientoCollectorService collectorService;

    public AyuntamientoCollectorScheduler(AyuntamientoCollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @Override
    public void start() {
        LOGGER.info("Starting one-off ayuntamiento collection");

        scheduler.submit(this::seed);
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping ayuntamiento seeder collection");
        scheduler.shutdown();
        if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
        }
    }

    public void seed() {
        collectorService.seed();
        LOGGER.info("Finished ayuntamiento seeder collection");
    }
}



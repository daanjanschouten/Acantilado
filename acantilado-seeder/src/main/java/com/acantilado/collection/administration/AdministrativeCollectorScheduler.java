package com.acantilado.collection.administration;


import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdministrativeCollectorScheduler implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdministrativeCollectorScheduler.class);

    private final ScheduledExecutorService scheduler;
    private final GeographicCollectorService collectorService;
    private final AdministrativeCollectorConfig config;

    public AdministrativeCollectorScheduler(
            GeographicCollectorService collectorService,
            AdministrativeCollectorConfig config) {
        this.collectorService = collectorService;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(config.getThreadPoolSize());
    }

    @Override
    public void start() {
        if (!config.isEnabled()) {
            LOGGER.info("Administrative collector is disabled, skipping startup");
            return;
        }

        if (!collectorService.isSeedingNecessary()) {
            LOGGER.info("Skipping administrative collection because data already populated");
            // Schedule periodic updates even if initial seed not needed
            schedulePeriodicCollection();
            return;
        }

        LOGGER.info("Starting administrative collection - initial seed required");

        // Run initial seed after configured delay
        scheduler.schedule(
                this::seedAndScheduleRecurring,
                config.getInitialDelay().toSeconds(),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping administrative collection");

        scheduler.shutdown();

        if (!scheduler.awaitTermination(config.getShutdownTimeout().toSeconds(), TimeUnit.SECONDS)) {
            LOGGER.warn("Scheduler did not terminate gracefully, forcing shutdown");
            scheduler.shutdownNow();
        }
    }

    private void seedAndScheduleRecurring() {
        try {
            seed();
            // After initial seed, schedule periodic updates
            schedulePeriodicCollection();
        } catch (Exception e) {
            LOGGER.error("Error during initial administrative data seeding", e);
        }
    }

    private void schedulePeriodicCollection() {
        LOGGER.info("Scheduling periodic administrative data collection every {}",
                config.getCollectionInterval());

        scheduler.scheduleAtFixedRate(
                this::seed,
                config.getCollectionInterval().toSeconds(),
                config.getCollectionInterval().toSeconds(),
                TimeUnit.SECONDS
        );
    }

    private void seed() {
        try {
            LOGGER.info("Starting administrative data collection");
            collectorService.seed();
            LOGGER.info("Finished administrative data collection");
        } catch (Exception e) {
            LOGGER.error("Error during administrative data collection", e);
        }
    }
}
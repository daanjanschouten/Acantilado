package com.acantilado.collection.administration;

import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdministrativeCollectorScheduler implements Managed {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AdministrativeCollectorScheduler.class);

  private final ExecutorService scheduler;
  private final GeographicCollectorService collectorService;
  private final AdministrativeCollectorConfig config;

  public AdministrativeCollectorScheduler(
      GeographicCollectorService collectorService, AdministrativeCollectorConfig config) {
    this.collectorService = collectorService;
    this.config = config;
    this.scheduler = Executors.newSingleThreadExecutor();
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      LOGGER.info("Administrative collector is disabled, skipping startup");
      return;
    }

    if (!collectorService.isSeedingNecessary()) {
      LOGGER.info("Skipping administrative collection because data already populated");
      return;
    }

    LOGGER.info("Starting administrative collection");
    scheduler.submit(this::seed);
  }

  @Override
  public void stop() throws Exception {
    LOGGER.info("Stopping administrative collection");

    scheduler.shutdown();

    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
      LOGGER.warn("Scheduler did not terminate gracefully, forcing shutdown");
      scheduler.shutdownNow();
    }
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

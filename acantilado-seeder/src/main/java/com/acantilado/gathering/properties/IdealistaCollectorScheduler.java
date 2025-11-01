package com.acantilado.gathering.properties;

import com.acantilado.gathering.properties.idealista.IdealistaPropertyType;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IdealistaCollectorScheduler implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollectorScheduler.class);

    private static final Set<String> PROVINCES = Set.of("Granada");
    private static final Set<IdealistaPropertyType> PROPERTY_TYPES = Set.of(IdealistaPropertyType.HOMES);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final IdealistaCollectorService collectorService;

    public IdealistaCollectorScheduler(IdealistaCollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @Override
    public void start() {
        LOGGER.info("Starting property seeder collection");

        scheduler.scheduleAtFixedRate(
                this::collectProperties,
                10,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping property seeder collection");

        scheduler.shutdown();
        collectorService.shutdownExecutor();
        if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
        }
    }

    private void collectProperties() {
        PROVINCES.forEach(provinceName -> {
            PROPERTY_TYPES.forEach(propertyType -> {
                try {
                    LOGGER.info("Start of scheduled real estate collection for province {} and property type {}",
                            provinceName,
                            propertyType);

                    if (collectorService.collectRealEstateForProvince(provinceName, propertyType)) {
                        LOGGER.info("Completed scheduled real estate collection for province {} and property type {}",
                                provinceName,
                                propertyType);
                    } else {
                        LOGGER.error("Partial completion of scheduled real estate collection for province {}", provinceName);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error during collection", e);
                }
            });
        });
    }

    private long calculateInitialDelay(int desiredHour) {
//        final int currentHour = Instant.now().atOffset(ZoneOffset.of(ZoneId.of("CET").getId())).getHour();
//        int hoursToGo = currentHour > desiredHour
//                ? 24 - (currentHour - desiredHour)
//                : desiredHour - currentHour;
        return 3600L;
    }
}
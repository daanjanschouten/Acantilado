package com.acantilado.gathering.properties;

import com.acantilado.gathering.properties.idealistaTypes.IdealistaPropertyType;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CONFIRM AYUNTAMIENTO MATCHING
 */

public class IdealistaCollectorScheduler implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollectorScheduler.class);

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
        Set<String> provinceNames = Set.of();
        Set<IdealistaPropertyType> propertyTypes = Set.of(IdealistaPropertyType.LANDS, IdealistaPropertyType.HOMES);

        provinceNames.forEach(provinceName -> {
            try {
                LOGGER.info("Start of scheduled collection for province {} and types {}", provinceName, propertyTypes);

                if(collectorService.collectPropertiesForProvinceName(provinceName, propertyTypes)) {
                    LOGGER.info("Completed scheduled property collection for province {}", provinceName);
                } else {
                    LOGGER.error("Partial completion of scheduled property collection for province {}", provinceName);
                }
            } catch (Exception e) {
                LOGGER.error("Error during scheduled property collection for province {}", provinceName, e);
            }
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
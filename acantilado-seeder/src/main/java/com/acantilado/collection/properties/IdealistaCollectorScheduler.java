package com.acantilado.collection.properties;

import com.acantilado.collection.properties.idealista.IdealistaPropertyType;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IdealistaCollectorScheduler implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollectorScheduler.class);

    private static final Set<String> PROVINCES = Set.of("Barcelona");
    private static final Set<IdealistaPropertyType> PROPERTY_TYPES = Set.of(IdealistaPropertyType.HOMES);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final IdealistaCollectorServiceFactory collectorServiceFactory;
    private final Set<IdealistaProvinceCollectorService> provinceCollectorServices = new HashSet<>();

    public IdealistaCollectorScheduler(IdealistaCollectorServiceFactory collectorServiceFactory) {
        this.collectorServiceFactory = collectorServiceFactory;
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
        provinceCollectorServices.forEach(IdealistaProvinceCollectorService::shutdownExecutor);
        if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
        }
    }

    private void collectProperties() {
        PROVINCES.forEach(provinceName -> {
            IdealistaProvinceCollectorService collectorService = collectorServiceFactory.getCollectorService(provinceName);
            provinceCollectorServices.add(collectorService);
            PROPERTY_TYPES.forEach(propertyType -> {
                try {
                    if (collectorService.collectRealEstateForProvince(propertyType)) {
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
            provinceCollectorServices.remove(collectorService);
        });
    }
}
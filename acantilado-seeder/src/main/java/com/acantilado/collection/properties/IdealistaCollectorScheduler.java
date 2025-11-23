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
import java.util.stream.Collectors;

public class IdealistaCollectorScheduler implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollectorScheduler.class);

    private final ScheduledExecutorService scheduler;
    private final IdealistaCollectorServiceFactory collectorServiceFactory;
    private final IdealistaCollectorConfig config;
    private final Set<IdealistaProvinceCollectorService> provinceCollectorServices = new HashSet<>();

    public IdealistaCollectorScheduler(
            IdealistaCollectorServiceFactory collectorServiceFactory,
            IdealistaCollectorConfig config) {
        this.collectorServiceFactory = collectorServiceFactory;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(config.getSchedulerThreadPoolSize());
    }

    @Override
    public void start() {
        if (!config.isEnabled()) {
            LOGGER.info("Idealista property collector is disabled, skipping startup");
            return;
        }

        LOGGER.info("Starting property collector for provinces: {} with property types: {}",
                config.getProvinces(),
                config.getPropertyTypes());

        scheduler.scheduleAtFixedRate(
                this::collectProperties,
                config.getInitialDelay().toSeconds(),
                config.getCollectionInterval().toSeconds(),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping property collector");

        scheduler.shutdown();
        provinceCollectorServices.forEach(IdealistaProvinceCollectorService::shutdownExecutor);

        if (!scheduler.awaitTermination(config.getShutdownTimeout().toSeconds(), TimeUnit.SECONDS)) {
            LOGGER.warn("Scheduler did not terminate gracefully, forcing shutdown");
            scheduler.shutdownNow();
        }
    }

    private void collectProperties() {
        Set<String> provinces = config.getProvinces();
        Set<IdealistaPropertyType> propertyTypes = parsePropertyTypes(config.getPropertyTypes());

        provinces.forEach(provinceName -> {
            IdealistaProvinceCollectorService collectorService =
                    collectorServiceFactory.getCollectorService(provinceName);
            provinceCollectorServices.add(collectorService);

            propertyTypes.forEach(propertyType -> {
                try {
                    LOGGER.info("Starting collection for province {} and property type {}",
                            provinceName, propertyType);

                    if (collectorService.collectRealEstateForProvince(propertyType)) {
                        LOGGER.info("Completed scheduled real estate collection for province {} and property type {}",
                                provinceName,
                                propertyType);
                    } else {
                        LOGGER.error("Partial completion of scheduled real estate collection for province {}",
                                provinceName);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error during collection for province {} and property type {}",
                            provinceName, propertyType, e);
                }
            });
            provinceCollectorServices.remove(collectorService);
        });
    }

    private Set<IdealistaPropertyType> parsePropertyTypes(Set<String> propertyTypeStrings) {
        return propertyTypeStrings.stream()
                .map(typeString -> {
                    try {
                        return IdealistaPropertyType.valueOf(typeString.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Invalid property type: {}, skipping", typeString);
                        return null;
                    }
                })
                .filter(type -> type != null)
                .collect(Collectors.toSet());
    }
}
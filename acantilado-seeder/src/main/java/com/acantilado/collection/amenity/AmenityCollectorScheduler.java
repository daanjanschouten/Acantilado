package com.acantilado.collection.amenity;

import com.acantilado.core.amenity.fields.AcantiladoAmenityType;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AmenityCollectorScheduler implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmenityCollectorScheduler.class);

    private final ScheduledExecutorService scheduler;
    private final AmenityCollectorServiceFactory collectorServiceFactory;
    private final AmenityCollectorConfig config;
    private final Set<AmenityProvinceCollectorService> provinceCollectorServices = new HashSet<>();

    public AmenityCollectorScheduler(AmenityCollectorServiceFactory factory, AmenityCollectorConfig config) {
        this.collectorServiceFactory = factory;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(config.getThreadPoolSize());
    }

    @Override
    public void start() {
        if (!config.isEnabled()) {
            LOGGER.info("Amenity collector is disabled, skipping startup");
            return;
        }

        LOGGER.info("Starting amenity collector for provinces: {} with chains: {}",
                config.getProvinces(),
                config.getSearchCategories());

        scheduler.scheduleAtFixedRate(
                this::collectAmenities,
                config.getInitialDelay().toSeconds(),
                config.getCollectionInterval().toSeconds(),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping amenity collector");

        scheduler.shutdown();
        provinceCollectorServices.forEach(AmenityProvinceCollectorService::shutdownExecutor);

        if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
            LOGGER.warn("Scheduler did not terminate gracefully, forcing shutdown");
            scheduler.shutdownNow();
        }
    }

    private void collectAmenities() {
        Set<String> provinceIds = config.getProvinces();
        Set<AcantiladoAmenityType> amenityChains = parseSearchCategories(config.getSearchCategories());

        provinceIds.forEach(provinceId -> {
            AmenityProvinceCollectorService collectorService =
                    collectorServiceFactory.getCollectorService(provinceId);
            provinceCollectorServices.add(collectorService);

            amenityChains.forEach(amenityType -> {
                try {
                    LOGGER.info("Starting collection for province {} and amenity type {}",
                            provinceId, amenityType);

                    if (collectorService.collectAmenitiesForProvince()) {
                        LOGGER.info("Completed scheduled amenity collection for province {} and type {}",
                                provinceId, amenityType);
                    } else {
                        LOGGER.error("Partial completion of scheduled amenity collection for province {} and type {}",
                                provinceId, amenityType);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error during collection for province {} and type {}",
                            provinceId, amenityType, e);
                }
            });

            provinceCollectorServices.remove(collectorService);
        });
    }

    private Set<AcantiladoAmenityType> parseSearchCategories(Set<String> categoryStrings) {
        return categoryStrings.stream()
                .map(categoryString -> {
                    try {
                        return AcantiladoAmenityType.valueOf(categoryString.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Invalid amenity chain: {}, skipping", categoryString);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
package com.acantilado.collection.amenity;

import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
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
                config.getAmenityChains());

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


        Set<AcantiladoAmenityChain> amenityChains = parseAmenityChains(config.getAmenityChains());

        provinceIds.forEach(provinceId -> {
            AmenityProvinceCollectorService collectorService =
                    collectorServiceFactory.getCollectorService(provinceId);
            provinceCollectorServices.add(collectorService);

            amenityChains.forEach(chain -> {
                try {
                    LOGGER.info("Starting collection for province {} and amenity chain {}",
                            provinceId, chain);

                    if (collectorService.collectAmenitiesForProvince()) {
                        LOGGER.info("Completed scheduled amenity collection for province {} and chain {}",
                                provinceId, chain);
                    } else {
                        LOGGER.error("Partial completion of scheduled amenity collection for province {}",
                                provinceId);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error during collection for province {} and chain {}",
                            provinceId, chain, e);
                }
            });

            provinceCollectorServices.remove(collectorService);
        });
    }

    private Set<AcantiladoAmenityChain> parseAmenityChains(Set<String> chainStrings) {
        return chainStrings.stream()
                .map(chainString -> {
                    try {
                        return AcantiladoAmenityChain.valueOf(chainString.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Invalid amenity chain: {}, skipping", chainString);
                        return null;
                    }
                })
                .filter(chain -> chain != null)
                .collect(Collectors.toSet());
    }
}
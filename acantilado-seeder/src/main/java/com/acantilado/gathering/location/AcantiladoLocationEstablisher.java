package com.acantilado.gathering.location;

import com.acantilado.core.administrative.*;
import com.acantilado.gathering.properties.utils.RetryableBatchedExecutor;
import org.hibernate.SessionFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AcantiladoLocationEstablisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcantiladoLocationEstablisher.class);

    private final ProvinciaDAO provinciaDAO;
    private final AyuntamientoDAO ayuntamientoDAO;
    private final BarrioDAO barrioDAO;
    private final CodigoPostalDAO codigoPostalDAO;
    private final IdealistaLocationMappingDAO mappingDAO;

    private final AtomicInteger locationsEstablished = new AtomicInteger();
    private final AtomicInteger bruteForceCodigoPostalMatches = new AtomicInteger();
    private final AtomicInteger barriosEstablished = new AtomicInteger();
    private final AtomicInteger barrioMisses = new AtomicInteger();

    private final AtomicBoolean bootstrapMode = new AtomicBoolean(false);

    private final Map<Long, Map<IdealistaLocationMapping, Integer>> ayuntamientoIdsByCountOfIdealistaLocationIds = new HashMap<>();
    private final Set<String> mappedLocationIds;

    public AcantiladoLocationEstablisher(Set<String> mappedLocationIds, ProvinciaDAO provinciaDao, AyuntamientoDAO ayuntamientoDAO, BarrioDAO barrioDAO, CodigoPostalDAO codigoPostalDAO, IdealistaLocationMappingDAO mappingDAO) {
        this.mappedLocationIds = mappedLocationIds;
        this.provinciaDAO = provinciaDao;
        this.ayuntamientoDAO = ayuntamientoDAO;
        this.barrioDAO = barrioDAO;
        this.codigoPostalDAO = codigoPostalDAO;
        this.mappingDAO = mappingDAO;
    }

    public void setBootstrapMode(boolean shouldBootstrap) {
        this.bootstrapMode.getAndSet(shouldBootstrap);
    }

    public AcantiladoLocation establishAndRecordMapping(String idealistaAyuntamiento, String idealistaLocationId, Point locationPoint, long propertyCode) {
        String normalizedLocationId = normalizeLocationId(idealistaLocationId);
//        Optional<Ayuntamiento> maybeAyuntamiento = establishAyuntamientoFromExistingMapping(normalizedLocationId);
//        if (maybeAyuntamiento.isPresent()) {
//            Ayuntamiento ayuntamiento = maybeAyuntamiento.get();
//            return buildAcantiladoLocation(ayuntamiento, locationPoint, propertyCode);
//        }

        Ayuntamiento ayuntamiento = establishAyuntamientoByCoordinates(locationPoint);
        if (bootstrapMode.get()) {
            updateInMemoryMapping(
                    ayuntamiento.getId(),
                    new IdealistaLocationMapping(normalizedLocationId, idealistaAyuntamiento, ayuntamiento.getId(), ayuntamiento.getName()));
        } else {
            LOGGER.error("Not in bootstrap mode but unable to find ayuntamiento for listing from existing mappings");
        }

        return buildAcantiladoLocation(ayuntamiento, locationPoint, propertyCode);
    }


    public Set<String> locationsToPopulate(SessionFactory sessionFactory, String provinceName) {
//        Provincia province = getProvinceFromName(provinciaDAO, sessionFactory, provinceName);
//        Set<Ayuntamiento> ayuntamientosForProvince = getAyuntamientosForProvince(ayuntamientoDAO, sessionFactory, province.getId());
//        Set<Long> ayuntamientoIdsForProvince = ayuntamientosForProvince.stream().map(Ayuntamiento::getId).collect(Collectors.toSet());
//
//        Map<Long, IdealistaLocationMapping> mappings = getMappingsForProvince(mappingDAO, sessionFactory, ayuntamientoIdsForProvince);
//
//        // Check if each ayuntamiento is associated with an Idealista location ID. If not, populate missing ones.
//        Set<Long> ayuntamientosMissing = findMissingMappings(ayuntamientoIdsForProvince, mappings.keySet());
//        if (!ayuntamientosMissing.isEmpty()) {
//            if ((double) ayuntamientosMissing.size() / ayuntamientoIdsForProvince.size() > 0.2) {
//                LOGGER.warn("Not enough existing mappings found, populating from scratch");
//                return Set.of(province.getIdealistaLocationId());
//            }
//
//            LOGGER.warn("Many ayuntamientos had mappings, only repopulating missing ones");
//            return ayuntamientosForProvince
//                    .stream()
//                    .filter(a -> ayuntamientosMissing.contains(a.getId()))
//                    .map(Ayuntamiento::getName)
//                    .collect(Collectors.toSet());
//        }
//
//        // Check if each mapping has enough confidence to rely on for regular collection
//        Set<String> ayuntamientosWithInsufficientConfidence = mappings.values()
//                .stream()
//                .filter(idealistaLocationMapping -> idealistaLocationMapping.getConfidenceScore() < 10)
//                .map(IdealistaLocationMapping::getIdealistaLocationId)
//                .collect(Collectors.toSet());
//        if (!ayuntamientosWithInsufficientConfidence.isEmpty()) {
//            LOGGER.warn("Some ayuntamientos had insufficient confidence scores, repopulating those");
//            return ayuntamientosWithInsufficientConfidence;
//        }

        LOGGER.info("Mappings complete - can do per-ayuntamiento search");
        return Set.of();
    }

    private static Map<Long, IdealistaLocationMapping> getMappingsForProvince(
            IdealistaLocationMappingDAO mappingDAO,
            SessionFactory sessionFactory,
            Set<Long> ayuntamientoIdsForProvince) {
        Map<Long, List<IdealistaLocationMapping>> mappings =
                RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction(sessionFactory,
                        () -> mappingDAO.findAll()
                                .stream()
                                .filter(mapped ->
                                        ayuntamientoIdsForProvince.contains(mapped.getAcantiladoAyuntamientoId()))
                                .collect(Collectors.groupingBy(IdealistaLocationMapping::getAcantiladoAyuntamientoId)));

        return mappings.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<IdealistaLocationMapping> list = entry.getValue();
                            if (list.size() != 1) {
                                LOGGER.error("Found more than one mapping for an ayuntamiento ID: {} {}",
                                        entry.getKey(), list);
                                throw new RuntimeException("Invalid mapping size");
                            }
                            return list.get(0);
                        }
                ));
    }

    private static Set<Long> findMissingMappings(Set<Long> ayuntamientoIds, Set<Long> mappedAyuntamientoIds) {
        Set<Long> missingIds = new HashSet<>(ayuntamientoIds);
        missingIds.removeAll(mappedAyuntamientoIds);

        Set<Long> extraIds = new HashSet<>(mappedAyuntamientoIds);
        extraIds.removeAll(ayuntamientoIds);

        if (!missingIds.isEmpty() || !extraIds.isEmpty()) {
            if (!missingIds.isEmpty()) {
                LOGGER.error("Missing mappings for ayuntamiento IDs: {}", missingIds);
                return missingIds;
            }

            LOGGER.error("Unexpected mappings for ayuntamiento IDs: {}", extraIds);
            throw new RuntimeException();
        }
        return Set.of();
    }

    private AcantiladoLocation buildAcantiladoLocation(Ayuntamiento ayuntamiento, Point locationPoint, long propertyCode) {
        CodigoPostal codigoPostal = findCodigoPostal(ayuntamiento, locationPoint);
        Barrio barrio = findBarrio(ayuntamiento, locationPoint, propertyCode);

        locationsEstablished.incrementAndGet();
        return Objects.isNull(barrio)
                ? new AcantiladoLocation(ayuntamiento, codigoPostal)
                : new AcantiladoLocation(ayuntamiento, codigoPostal, barrio);
    }

    private Ayuntamiento establishAyuntamientoByCoordinates(Point locationPoint) {
        List<Ayuntamiento> allAyuntamientos = ayuntamientoDAO.findAll();

        for (Ayuntamiento ayuntamiento : allAyuntamientos) {
            if (ayuntamiento.getGeometry().contains(locationPoint)) {
                LOGGER.debug("Point is inside ayuntamiento {}", ayuntamiento.getName());
                return ayuntamiento;
            }
        }

        LOGGER.debug("Point not contained in any ayuntamiento, using closest match");
        Optional<Ayuntamiento> maybeClosestAyuntamiento = allAyuntamientos.stream()
                .min(Comparator.comparingDouble(a -> a.getGeometry().distance(locationPoint)))
                .map(closest -> {
                    LOGGER.debug("Selecting closest ayuntamiento {}", closest.getName());
                    return closest;
                });

        return maybeClosestAyuntamiento.orElseThrow();
    }

    private Optional<Ayuntamiento> establishAyuntamientoFromExistingMapping(String locationId) {
        if (!mappedLocationIds.contains(locationId)) {
            return Optional.empty();
        }

        // If the idealistaLocationId is present in our mapping table, we should just use that.
        LOGGER.info("Found location ID in mapping table {}", locationId);
        Optional<IdealistaLocationMapping> maybeMapping =
                mappingDAO.findByIdealistaLocationId(locationId);

        if (maybeMapping.isPresent()) {
            long ayuntamientoId = maybeMapping.get().getAcantiladoAyuntamientoId();
            return ayuntamientoDAO.findById(ayuntamientoId);
        }
        return Optional.empty();
    }

    private void updateInMemoryMapping(Long ayuntamientoId, IdealistaLocationMapping mapping) {
        if (ayuntamientoIdsByCountOfIdealistaLocationIds.containsKey(ayuntamientoId)) {
            Map<IdealistaLocationMapping, Integer> locationIdByCounts = ayuntamientoIdsByCountOfIdealistaLocationIds.get(ayuntamientoId);
            if (locationIdByCounts.containsKey(mapping)) {
                locationIdByCounts.compute(mapping, (k, existingCount) -> existingCount + 1);
            } else {
                locationIdByCounts.put(mapping, 1);
            }
        } else {
            Map<IdealistaLocationMapping, Integer> newMap = new HashMap<>();
            newMap.put(mapping, 1);
            ayuntamientoIdsByCountOfIdealistaLocationIds.put(ayuntamientoId, newMap);
        }
    }

    private static int confidenceScore(long hits, double portion) {
        return (int) (hits * (portion * portion));
    }

    public void storeMappings() {
        Set<IdealistaLocationMapping> locationMappings = new HashSet<>();

        ayuntamientoIdsByCountOfIdealistaLocationIds.forEach((ayuntamientoId, countsByLocation) -> {
            if (countsByLocation.size() == 1) {
                countsByLocation.forEach((location, count) -> {
                    location.setConfidenceScore(confidenceScore(count, 1));
                    locationMappings.add(location);
                    LOGGER.debug("SUCCESS: found a single entry for ayuntamiento ID {}: {} with {} hits",
                            ayuntamientoId, location, count);
                });
            } else {
                countsByLocation.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .ifPresent(maxEntry -> {
                            IdealistaLocationMapping winningLocation = maxEntry.getKey();
                            int winningCount = maxEntry.getValue();
                            int totalCount = countsByLocation.values().stream().mapToInt(Integer::intValue).sum();
                            double portion = ((double) winningCount / totalCount);

                            winningLocation.setConfidenceScore(confidenceScore(winningCount, portion));
                            locationMappings.add(winningLocation);

                            LOGGER.debug("CONFLICT: Multiple locations for ayuntamiento {}, using most common: {} ({}/{} properties, {}%)",
                                    ayuntamientoId,
                                    winningLocation,
                                    winningCount,
                                    totalCount,
                                    portion);

                            countsByLocation.forEach((location, count) -> {
                                if (!location.equals(winningLocation)) {
                                    LOGGER.debug("  Rejected: {} ({} properties)", location, count);
                                }
                            });
                        });
            }
        });

        LOGGER.info("Final in-memory mapping: {}.", locationMappings);

        locationMappings.forEach(mapping -> {
            List<IdealistaLocationMapping> existingMappings = mappingDAO.findByAyuntamientoId(mapping.getAcantiladoAyuntamientoId());
            if (!existingMappings.isEmpty()) {
                LOGGER.error("Found an existing mapping {} for mapping {}, skipping", existingMappings, mapping);
            } else {
                mappingDAO.saveOrUpdate(mapping);
                LOGGER.info("Created new mapping: {} {} → {} ({}) with score {}",
                        mapping.getAcantiladoAyuntamientoId(),
                        mapping.getAcantiladoMunicipalityName(),
                        mapping.getIdealistaMunicipalityName(),
                        mapping.getIdealistaLocationId(),
                        mapping.getConfidenceScore());
            }
        });
    }

    private static String normalizeLocationId(String idealistaLocationId) {
        // Idealista location ID structure:
        // 0-EU-ES-{province}-{comarca}-{subgroup}-{ayuntamiento} = 7 segments (ayuntamiento level - keep as-is)
        // 0-EU-ES-{province}-{comarca}-{subgroup}-{ayuntamiento}-{district} = 8 segments (truncate to 7)
        // 0-EU-ES-{province}-{comarca}-{subgroup}-{ayuntamiento}-{district}-{subdistrict} = 9 segments (truncate to 7)

        String[] parts = idealistaLocationId.split("-");

        if (parts.length <= 7) {
            return idealistaLocationId;
        }

        // Has 8+ segments (includes district/neighborhood info) - keep only first 7
        return String.join("-", Arrays.copyOfRange(parts, 0, 7));
    }

    public int getBruteForceCodigoPostalMatches() {
        return bruteForceCodigoPostalMatches.get();
    }

    public int getLocationsEstablished() {
        return locationsEstablished.get();
    }

    public AtomicInteger getBarriosEstablished() {
        return barriosEstablished;
    }

    public AtomicInteger getBarrioMisses() {
        return barrioMisses;
    }

    private CodigoPostal findCodigoPostal(Ayuntamiento ayuntamiento, Point point) {
        for (CodigoPostal codigoPostal : ayuntamiento.getCodigosPostales()) {
            if (codigoPostal.getGeometry().covers(point)) {
                return codigoPostal;
            }
        }

        double minDistance = Double.MAX_VALUE;
        CodigoPostal closest = null;
        for (CodigoPostal codigoPostal : codigoPostalDAO.findAll()) {
            double distance = point.distance(codigoPostal.getGeometry());
            if (distance < minDistance) {
                minDistance = distance;
                closest = codigoPostal;
            }
        }

        LOGGER.debug("Found codigo postal {} by proximity of ~{}m", closest.getCodigoPostal(), (int) (minDistance * 111000));
        bruteForceCodigoPostalMatches.incrementAndGet();
        return closest;
    }

    private Barrio findBarrio(Ayuntamiento ayuntamiento, Point point, long propertyCode) {
        Set<Long> codesOfCitiesWithBarrios = new HashSet<>();
        for (CityAyuntamientoCodes code : CityAyuntamientoCodes.values()) {
            codesOfCitiesWithBarrios.add(code.cityCode);
        }

        if (codesOfCitiesWithBarrios.contains(ayuntamiento.getId())) {
            for (Barrio barrio : barrioDAO.findByAyuntamiento(ayuntamiento.getId())) {
                if (barrio.getGeometry().contains(point)) {
                    LOGGER.debug("Successfully found barrio for ayuntamiento {} and {}", ayuntamiento.getId(), propertyCode);
                    barriosEstablished.incrementAndGet();
                    return barrio;
                }
            }
            barrioMisses.incrementAndGet();
            LOGGER.debug("Expected a barrio but none found for {} and {}", ayuntamiento.getId(), propertyCode);
        }

        return null;
    }
}
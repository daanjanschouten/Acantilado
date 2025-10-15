package com.acantilado.gathering.administration.barrio;

import com.acantilado.core.administrative.Barrio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BarrioCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(BarrioCollector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String geoJsonPath;
    private final Long ayuntamientoId;
    private final String barrioTitle;

    public BarrioCollector(BarrioCollectorService.CityBarrioConfig cityBarrioConfig) {
        this.geoJsonPath = cityBarrioConfig.geoJsonPath();
        this.ayuntamientoId = cityBarrioConfig.ayuntamientoId();
        this.barrioTitle = cityBarrioConfig.barrioTitle();
    }

    public Collection<Barrio> collect() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(geoJsonPath);
            if (inputStream == null) {
                LOGGER.warn("Could not find barrio file: {}", geoJsonPath);
                return Collections.emptySet();
            }

            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            JsonNode features = MAPPER.readTree(reader).get("features");
            if (features == null || !features.isArray()) {
                throw new RuntimeException("Invalid GeoJSON: no features array found in " + geoJsonPath);
            }

            Set<Barrio> barrios = new HashSet<>();
            GeoJsonReader geoJsonReader = new GeoJsonReader();

            Iterator<JsonNode> featureIterator = features.elements();
            while (featureIterator.hasNext()) {
                JsonNode feature = featureIterator.next();
                Optional<Barrio> barrio = parseFeature(feature, geoJsonReader);
                barrio.ifPresent(barrios::add);
            }

            LOGGER.debug("Successfully parsed {} barrios from {}", barrios.size(), geoJsonPath);
            return barrios;

        } catch (Exception e) {
            LOGGER.error("Failed to load barrios from {}", geoJsonPath, e);
            throw new RuntimeException("Failed to load barrios from " + geoJsonPath, e);
        }
    }

    private Optional<Barrio> parseFeature(JsonNode feature, GeoJsonReader reader) {
        JsonNode properties;
        JsonNode geometry;
        try {
            // Check if this is Valencia format (properties at root, geo_shape wrapper)
            if (feature.has("geo_shape")) {
                properties = feature;
                geometry = feature.get("geo_shape").get("geometry");
            } else {
                properties = feature.get("properties");
                geometry = feature.get("geometry");
            }

            if (geometry == null) {
                LOGGER.warn("Feature missing geometry: {}", feature.toString().substring(0, Math.min(200, feature.toString().length())));
                return Optional.empty();
            }

            String name = getTextOrNull(properties, barrioTitle);
            if (name == null || name.trim().isEmpty()) {
                LOGGER.warn("Barrio missing name, skipping. Properties: {}", properties.toString().substring(0, Math.min(200, properties.toString().length())));
                return Optional.empty();
            }

            Geometry geometryString = reader.read(geometry.toString());
            return Optional.of(new Barrio(name, ayuntamientoId, geometryString));

        } catch (Exception e) {
            LOGGER.warn("Failed to parse barrio feature: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asText() : null;
    }
}
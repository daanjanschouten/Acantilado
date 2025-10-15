package com.acantilado.gathering.administration.codigopostal;

import com.acantilado.core.administrative.CodigoPostal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CodigoPostalCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodigoPostalCollector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GEOJSON_ZIP_FILE = "/postcodes/codigos_postales.zip";
    private static final String GEOJSON_FILENAME = "codigos_postales.geojson";

    public Iterator<Set<CodigoPostal>> seed() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(GEOJSON_ZIP_FILE);
            if (inputStream == null) {
                throw new RuntimeException("Could not find " + GEOJSON_ZIP_FILE);
            }

            // Unzip and read the GeoJSON file
            InputStream geoJsonStream = extractGeoJsonFromZip(inputStream);

            JsonNode root = MAPPER.readTree(geoJsonStream);
            JsonNode features = root.get("features");

            if (features == null || !features.isArray()) {
                throw new RuntimeException("Invalid GeoJSON: no features array found");
            }

            LOGGER.info("Found {} postal code features", features.size());

            Set<CodigoPostal> codigosPostales = new HashSet<>();
            GeoJsonReader geoJsonReader = new GeoJsonReader();

            Iterator<JsonNode> featureIterator = features.elements();
            while (featureIterator.hasNext()) {
                JsonNode feature = featureIterator.next();
                Optional<CodigoPostal> codigoPostal = parseFeature(feature, geoJsonReader);
                codigoPostal.ifPresent(codigosPostales::add);
            }

            LOGGER.info("Successfully parsed {} postal codes", codigosPostales.size());

            // Return as single collection
            return Collections.singleton(codigosPostales).iterator();

        } catch (Exception e) {
            LOGGER.error("Failed to load postal codes", e);
            throw new RuntimeException("Failed to load postal codes", e);
        }
    }

    private InputStream extractGeoJsonFromZip(InputStream zipInputStream) throws Exception {
        ZipInputStream zis = new ZipInputStream(zipInputStream);
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equals(GEOJSON_FILENAME) ||
                    entry.getName().endsWith(".geojson")) {
                LOGGER.debug("Found GeoJSON file in zip: {}", entry.getName());
                return zis;
            }
            zis.closeEntry();
        }

        throw new RuntimeException("No .geojson file found in zip archive");
    }

    private Optional<CodigoPostal> parseFeature(JsonNode feature, GeoJsonReader reader) {
        try {
            JsonNode properties = feature.get("properties");
            JsonNode geometry = feature.get("geometry");

            if (properties == null || geometry == null) {
                LOGGER.warn("Feature missing properties or geometry: {}", feature);
                return Optional.empty();
            }

            String codigoPostal = properties.get("COD_POSTAL").asText();
            String geometryJson = geometry.toString();
            Geometry geom = reader.read(geometryJson);

            return Optional.of(new CodigoPostal(codigoPostal, geom));

        } catch (Exception e) {
            LOGGER.warn("Failed to parse postal code feature: {}", feature, e);
            return Optional.empty();
        }
    }
}
package com.acantilado.collection.administration.codigopostal;

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

    public record CodigoIneAndPostalCode(String codigoIne, String codigoPostal, Geometry geometry) {};

    public Iterator<Set<CodigoPostal>> seed() {
        try {
            Set<CodigoPostal> codigosPostales = new HashSet<>();

            InputStream inputStream = getClass().getResourceAsStream(GEOJSON_ZIP_FILE);
            if (inputStream == null) {
                throw new RuntimeException("Could not find " + GEOJSON_ZIP_FILE);
            }
            InputStream geoJsonStream = extractGeoJsonFromZip(inputStream);
            JsonNode features = MAPPER.readTree(geoJsonStream).get("features");

            LOGGER.info("Found {} postal code features", features.size());
            GeoJsonReader geoJsonReader = new GeoJsonReader();

            Iterator<JsonNode> featureIterator = features.elements();
            while (featureIterator.hasNext()) {
                CodigoIneAndPostalCode feature = parseFeature(featureIterator.next(), geoJsonReader);
                codigosPostales.add(
                        new CodigoPostal(feature.codigoIne, feature.codigoPostal, feature.geometry));
            }

            LOGGER.info("Successfully parsed {} postal codes from {} features", codigosPostales.size(), features.size());
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

    private CodigoIneAndPostalCode parseFeature(JsonNode feature, GeoJsonReader reader) {
        try {
            JsonNode properties = feature.get("properties");
            JsonNode geometry = feature.get("geometry");

            if (properties == null || geometry == null) {
                throw new RuntimeException("Feature missing properties or geometry" + feature);
            }

            String geometryJson = geometry.toString();
            Geometry geom = reader.read(geometryJson);

            return new CodigoIneAndPostalCode (
                    properties.get("CODIGO_INE").asText(),
                    properties.get("COD_POSTAL").asText(),
                    geom);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse postal code feature" + feature, e);
        }
    }
}
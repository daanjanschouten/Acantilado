package com.acantilado.collection.administration.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Abstract collector for Spanish administrative units from local GeoJSON files.
 * Handles reading GeoJSON FeatureCollections and constructing entity objects.
 * Persistence is delegated to a higher-level service.
 */
public abstract class AdministrativeUnitCollector<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdministrativeUnitCollector.class);

    protected static final String NATCODE_FIELD = "NATCODE";
    protected static final String NAME_FIELD = "NAMEUNIT";

    private final ObjectMapper mapper;

    private static final String INPUT_DIRECTORY = "./acantilado-seeder/src/main/resources/administrative/";
    private static final String FEATURES = "features";
    private static final String PROPERTIES = "properties";
    private static final String GEOMETRY = "geometry";

    private final String datasetName;

    protected abstract T constructFromFeature(JsonNode feature);

    public AdministrativeUnitCollector(String datasetName) {
        this.mapper = new ObjectMapper();
        this.datasetName = datasetName;
    }

    public final Set<T> seed() {
        File inputFile = new File(INPUT_DIRECTORY, datasetName);

        if (!inputFile.exists()) {
            LOGGER.error("No GeoJSON found for {}", inputFile.getAbsolutePath());
            throw new IllegalStateException();
        }

        try {
            JsonNode rootNode = mapper.readTree(inputFile);
            JsonNode featuresNode = rootNode.get(FEATURES);
            if (featuresNode == null || !featuresNode.isArray()) {
                LOGGER.error("Invalid GeoJSON: missing or invalid 'features' array");
                return new HashSet<>();
            }

            Set<T> entities = parseFeatures(featuresNode);
            if (entities.isEmpty()) {
                LOGGER.warn("No entities were successfully parsed from features");
                return new HashSet<>();
            }

            LOGGER.info("Successfully parsed {} entities from {}", entities.size(), datasetName);
            return entities;

        } catch (IOException e) {
            LOGGER.error("Failed to read or parse GeoJSON file: {}", inputFile.getAbsolutePath(), e);
            return new HashSet<>();
        } catch (Exception e) {
            LOGGER.error("Unexpected error during parsing from file: {}", inputFile.getAbsolutePath(), e);
            return new HashSet<>();
        }
    }

    private Set<T> parseFeatures(JsonNode featuresNode) {
        Set<T> entities = new HashSet<>();
        Iterator<JsonNode> featureIterator = featuresNode.elements();

        while (featureIterator.hasNext()) {
            JsonNode feature = featureIterator.next();

            try {
                T entity = constructFromFeature(feature);
                entities.add(entity);
            } catch (Exception e) {
                LOGGER.error("Failed to construct entity from feature", e);
                throw new IllegalStateException();
            }
        }

        return entities;
    }

    protected JsonNode getProperties(JsonNode feature) {
        JsonNode properties = feature.get(PROPERTIES);
        if (properties == null) {
            throw new IllegalArgumentException("Feature missing 'properties' field");
        }
        return properties;
    }

    protected JsonNode getGeometry(JsonNode feature) {
        JsonNode geometry = feature.get(GEOMETRY);
        if (geometry == null) {
            throw new IllegalArgumentException("Feature missing 'geometry' field");
        }
        return geometry;
    }

    protected String getStringValue(JsonNode properties, String fieldName) {
        JsonNode node = properties.get(fieldName);
        return (node != null && !node.isNull()) ? node.asText() : null;
    }

    protected Long getLongValue(JsonNode properties, String fieldName) {
        JsonNode node = properties.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isNumber()) {
            return node.asLong();
        } else if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException e) {
                LOGGER.warn("Could not parse {} as long: {}", fieldName, node.asText());
                return null;
            }
        }

        return null;
    }

    /**
     * Gets the ObjectMapper for use by subclasses if needed.
     *
     * @return The Jackson ObjectMapper
     */
    protected ObjectMapper getMapper() {
        return mapper;
    }
}
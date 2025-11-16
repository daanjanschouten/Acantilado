package com.acantilado.collection.administration.admin;

import com.acantilado.core.administrative.Ayuntamiento;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collector for Ayuntamiento (municipality) data from local GeoJSON files.
 */
public class AyuntamientoCollector extends AdministrativeUnitCollector<Ayuntamiento> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AyuntamientoCollector.class);

    private static final String DATASET_NAME = "municipalities.geojson";

    public AyuntamientoCollector() {
        super(DATASET_NAME);
    }

    @Override
    protected Ayuntamiento constructFromFeature(JsonNode feature) {
        try {
            JsonNode properties = getProperties(feature);
            JsonNode geometry = getGeometry(feature);

            NatCode natCode = NatCode.parse(getStringValue(properties,  AdministrativeUnitCollector.NATCODE_FIELD));
            String municipalityName = getStringValue(properties, AdministrativeUnitCollector.NAME_FIELD);

            // Parse geometry
            String geoJsonString = geometry.toString();
            return getAyuntamiento(geoJsonString, natCode, municipalityName);
        } catch (Exception e) {
            LOGGER.error("Failed to construct ayuntamiento from feature", e);
            throw new RuntimeException("Failed to construct ayuntamiento", e);
        }
    }

    private static Ayuntamiento getAyuntamiento(String geoJsonString, NatCode natCode, String municipalityName) throws ParseException {
        GeoJsonReader reader = new GeoJsonReader();
        Geometry geom = reader.read(geoJsonString);

        Ayuntamiento ayuntamiento = new Ayuntamiento(
                natCode.getIneCode(),
                municipalityName,
                natCode.getProvinciaId(),
                natCode.getComunidadAutonomaId(),
                geom
        );

        // Serialize geometry for persistence (as required by the entity)
        GeoJsonWriter writer = new GeoJsonWriter();
        ayuntamiento.setGeometryJson(writer.write(geom));
        return ayuntamiento;
    }
}
package com.acantilado.collection.administration.admin;

import com.acantilado.core.administrative.Provincia;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collector for Provincia data from local GeoJSON files.
 */
public class ProvinciaCollector extends AdministrativeUnitCollector<Provincia> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvinciaCollector.class);

    private static final String DATASET_NAME = "provinces.geojson";

    public ProvinciaCollector() {
        super(DATASET_NAME);
    }

    @Override
    protected Provincia constructFromFeature(JsonNode feature) {
        try {
            JsonNode properties = getProperties(feature);
            JsonNode geometry = getGeometry(feature);

            NatCode natCode = NatCode.parse(getStringValue(properties,  AdministrativeUnitCollector.NATCODE_FIELD));
            String provinceName = getStringValue(properties, AdministrativeUnitCollector.NAME_FIELD);

            String geoJsonString = geometry.toString();
            return getProvincia(geoJsonString, natCode, provinceName);
        } catch (Exception e) {
            LOGGER.error("Failed to construct provincia from feature", e);
            throw new RuntimeException("Failed to construct provincia", e);
        }
    }

    private static Provincia getProvincia(String geoJsonString, NatCode natCode, String provinciaName) throws ParseException {
        GeoJsonReader reader = new GeoJsonReader();
        Geometry geom = reader.read(geoJsonString);

        Provincia provincia = new Provincia(
                natCode.getProvinciaId(),
                provinciaName,
                natCode.getComunidadAutonomaId(),
                geom);

        // Serialize geometry for persistence (as required by the entity)
        GeoJsonWriter writer = new GeoJsonWriter();
        provincia.setGeometryJson(writer.write(geom));
        return provincia;
    }
}
package com.acantilado.gathering.administration.ayuntamiento;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.ComunidadAutonoma;
import com.acantilado.core.administrative.Provincia;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class AyuntamientoCollector extends OpenDataSoftCollector<Ayuntamiento> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AyuntamientoCollector.class);
    private static final String DATASET_NAME = "georef-spain-municipio";

    public AyuntamientoCollector() {
        super(DATASET_NAME);
    }

    @Override
    protected Optional<Ayuntamiento> constructObject(JsonNode jsonNode) {
        try {
            final long provinciaId = jsonNode.get("prov_code").asLong();
            Provincia provincia = new Provincia(
                    provinciaId,
                    jsonNode.get("prov_name").textValue()
            );

            ComunidadAutonoma comunidadAutonoma = new ComunidadAutonoma(
                    jsonNode.get("acom_code").asLong(),
                    jsonNode.get("acom_name").textValue()
            );

            JsonNode geoShape = jsonNode.get("geo_shape");
            String geoJsonString = geoShape.toString();
            GeoJsonReader reader = new GeoJsonReader();
            Geometry geometry = reader.read(geoJsonString);

            Ayuntamiento ayuntamiento = new Ayuntamiento(
                    jsonNode.get("mun_code").asLong(),
                    jsonNode.get("mun_name").textValue(),
                    provinciaId,
                    provincia,
                    comunidadAutonoma,
                    geometry);

            // Write here because merge() in Dao doesn't work with transient settings.
            GeoJsonWriter writer = new GeoJsonWriter();
            ayuntamiento.setGeometryJson(writer.write(geometry));

            return Optional.of(ayuntamiento);
        } catch (Exception e) {
            LOGGER.info("Failed to construct Ayuntamiento: {}", jsonNode);
            return Optional.empty();
        }
    }
}
package com.acantilado.collection.administration.admin;

import com.acantilado.core.administrative.ComunidadAutonoma;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Collector for Comunidad Autonoma data from local GeoJSON files. */
public class ComunidadAutonomaCollector extends AdministrativeUnitCollector<ComunidadAutonoma> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ComunidadAutonomaCollector.class);

  private static final String DATASET_NAME = "comunidades.geojson";

  public ComunidadAutonomaCollector() {
    super(DATASET_NAME);
  }

  @Override
  protected ComunidadAutonoma constructFromFeature(JsonNode feature) {
    try {
      JsonNode properties = getProperties(feature);
      JsonNode geometry = getGeometry(feature);

      NatCode natCode =
          NatCode.parse(getStringValue(properties, AdministrativeUnitCollector.NATCODE_FIELD));
      String provinceName = getStringValue(properties, AdministrativeUnitCollector.NAME_FIELD);

      String geoJsonString = geometry.toString();
      return getComunidadAutonoma(geoJsonString, natCode, provinceName);
    } catch (Exception e) {
      LOGGER.error("Failed to construct CA from feature", e);
      throw new RuntimeException("Failed to construct CA", e);
    }
  }

  private static ComunidadAutonoma getComunidadAutonoma(
      String geoJsonString, NatCode natCode, String caName) throws ParseException {
    GeoJsonReader reader = new GeoJsonReader();
    Geometry geom = reader.read(geoJsonString);

    ComunidadAutonoma comunidadAutonoma =
        new ComunidadAutonoma(natCode.getComunidadAutonomaId(), caName, geom);

    // Serialize geometry for persistence (as required by the entity)
    GeoJsonWriter writer = new GeoJsonWriter();
    comunidadAutonoma.setGeometryJson(writer.write(geom));
    return comunidadAutonoma;
  }
}

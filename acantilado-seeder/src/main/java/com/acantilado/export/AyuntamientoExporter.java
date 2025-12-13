package com.acantilado.export;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.utils.RetryableBatchedExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.hibernate.SessionFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AyuntamientoExporter {
  private static final Logger LOGGER = LoggerFactory.getLogger(AyuntamientoExporter.class);

  private final SessionFactory sessionFactory;
  private final ObjectMapper objectMapper;
  private final GeoJsonWriter geoJsonWriter;

  public AyuntamientoExporter(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
    this.objectMapper = new ObjectMapper();
    this.geoJsonWriter = new GeoJsonWriter();
  }

  /**
   * Exports all ayuntamientos for a given province as a GeoJSON FeatureCollection
   *
   * @param provinceId The province ID to export
   * @param outputFile The output file path
   */
  public void exportProvinceAsGeoJson(String provinceId, String outputFile) {
    LOGGER.info("Starting export for province {}", provinceId);

    RetryableBatchedExecutor.executeRunnableInSessionWithTransaction(
        sessionFactory,
        () -> {
          try {
            AyuntamientoDAO ayuntamientoDAO = new AyuntamientoDAO(sessionFactory);
            List<Ayuntamiento> ayuntamientos = ayuntamientoDAO.findByProvinceId(provinceId);

            if (ayuntamientos.isEmpty()) {
              throw new IllegalArgumentException(
                  "No ayuntamientos found for province ID: " + provinceId);
            }

            LOGGER.info("Found {} ayuntamientos, starting export", ayuntamientos.size());

            // Stream to file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
              // Write FeatureCollection opening
              writer.write("{\n");
              writer.write("  \"type\": \"FeatureCollection\",\n");
              writer.write("  \"features\": [\n");

              int featureCount = 0;

              for (Ayuntamiento ayuntamiento : ayuntamientos) {
                Geometry geometry = ayuntamiento.getGeometry();

                if (geometry == null) {
                  LOGGER.warn("Ayuntamiento {} has no geometry", ayuntamiento.getId());
                  continue;
                }

                // Write comma separator if not the first feature
                if (featureCount > 0) {
                  writer.write(",\n");
                }

                // Build feature object
                ObjectNode feature = objectMapper.createObjectNode();
                feature.put("type", "Feature");

                // Add geometry
                String geometryJson = geoJsonWriter.write(geometry);
                try {
                  feature.set("geometry", objectMapper.readTree(geometryJson));
                } catch (JsonProcessingException e) {
                  LOGGER.error(
                      "Failed to parse geometry for ayuntamiento {}", ayuntamiento.getId(), e);
                  continue;
                }

                // Add properties
                ObjectNode properties = feature.putObject("properties");
                properties.put("ayuntamientoId", ayuntamiento.getId());
                properties.put("name", ayuntamiento.getName());
                properties.put("provinciaId", ayuntamiento.getProvinciaId());

                if (ayuntamiento.getPhone() != null) {
                  properties.put("phone", ayuntamiento.getPhone());
                }

                // Add centroid if available
                if (ayuntamiento.getCentroid() != null) {
                  ObjectNode centroid = properties.putObject("centroid");
                  centroid.put("lon", ayuntamiento.getCentroid().get("lon"));
                  centroid.put("lat", ayuntamiento.getCentroid().get("lat"));
                }

                // Write feature with indentation
                String featureJson =
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(feature);
                // Indent the feature JSON
                String[] lines = featureJson.split("\n");
                for (int j = 0; j < lines.length; j++) {
                  writer.write("    " + lines[j]);
                  if (j < lines.length - 1) {
                    writer.write("\n");
                  }
                }

                featureCount++;
              }

              // Close the features array and FeatureCollection
              writer.write("\n  ]\n");
              writer.write("}\n");

              writer.flush();

              LOGGER.info("Export complete: {} features written to {}", featureCount, outputFile);

            } catch (IOException e) {
              LOGGER.error("IO error during export", e);
              throw new RuntimeException("Failed to write GeoJSON file", e);
            }

          } catch (Exception e) {
            LOGGER.error("Export failed with exception", e);
            throw new RuntimeException("Export failed", e);
          }
        });

    LOGGER.info("Export task submitted");
  }

  public void exportProvince29(String outputFile) {
    exportProvinceAsGeoJson("29", outputFile);
  }
}

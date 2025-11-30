package com.acantilado.core.administrative;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "comunidad_autonoma")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.comunidadautonoma.findAll",
                        query = "SELECT c FROM ComunidadAutonoma c"
                )
        }
)
public class ComunidadAutonoma {

    @Id
    @Column(name = "comunidadAutonomaId")
    private String comunidadAutonomaId;

    @Column(name = "name", nullable = false)
    private String name;

    /** ---------- GEOMETRY FIELDS (same pattern as Provincia) ------------- */

    @JsonIgnore
    @Column(name = "geometry", columnDefinition = "CLOB")
    private String geometryJson;

    @JsonIgnore
    @Transient
    private Geometry geometry;

    public ComunidadAutonoma() {}

    public ComunidadAutonoma(String comunidadAutonomaId, String name, Geometry geometry) {
        this.comunidadAutonomaId = comunidadAutonomaId;
        this.name = name;
        this.geometry = geometry;
    }

    public String getId() { return comunidadAutonomaId; }
    public String getName() { return name; }

    public void setId(String id) { this.comunidadAutonomaId = id; }
    public void setName(String name) { this.name = name; }

    /* ---------------- GEOMETRY GETTERS ---------------- */

    public Geometry getGeometry() { return geometry; }
    public String getGeometryJson() { return geometryJson; }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public void setGeometryJson(String geometryJson) {
        this.geometryJson = geometryJson;
        this.geometry = null; // force re-parse on load
    }

    /** Geometry-derived properties for JSON exposure */

    @JsonProperty("bounds")
    public Map<String, Double> getBounds() {
        if (geometry == null) return null;
        Envelope e = geometry.getEnvelopeInternal();
        return Map.of(
                "minLon", e.getMinX(),
                "maxLon", e.getMaxX(),
                "minLat", e.getMinY(),
                "maxLat", e.getMaxY()
        );
    }

    @JsonProperty("centroid")
    public Map<String, Double> getCentroid() {
        if (geometry == null) return null;
        Point p = geometry.getCentroid();
        return Map.of(
                "lon", p.getX(),
                "lat", p.getY()
        );
    }

    /** ---------- LIFECYCLE HOOKS (same as Provincia) ----------- */

    @PrePersist
    @PreUpdate
    private void serializeGeometry() {
        if (geometry != null) {
            try {
                org.locationtech.jts.io.geojson.GeoJsonWriter writer =
                        new org.locationtech.jts.io.geojson.GeoJsonWriter();
                this.geometryJson = writer.write(geometry);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to serialize geometry for comunidad: " + comunidadAutonomaId, e
                );
            }
        }
    }

    @PostLoad
    private void parseGeometry() {
        if (geometryJson != null && !geometryJson.isEmpty()) {
            try {
                GeoJsonReader reader = new GeoJsonReader();
                this.geometry = reader.read(geometryJson);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to parse geometry for comunidad: " + comunidadAutonomaId, e
                );
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(comunidadAutonomaId, name);
    }

    @Override
    public String toString() {
        return "ComunidadAutonoma{" +
                "comunidadAutonomaId='" + comunidadAutonomaId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

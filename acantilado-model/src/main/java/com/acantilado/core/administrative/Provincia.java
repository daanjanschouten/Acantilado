package com.acantilado.core.administrative;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import javax.persistence.*;
import java.util.Map;

@Entity
@Table(name = "PROVINCIA")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.provincia.findAll",
                        query = "SELECT p FROM Provincia p"
                ),
                @NamedQuery(
                        name = "com.acantilado.provincia.findByName",
                        query = "SELECT p FROM Provincia p WHERE p.name = :name"
                )
        }
)
public class Provincia {

    @Id
    @Column(name = "provinciaId")
    private String provinciaId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "comunidadAutonomaId", nullable = false)
    private String comunidadAutonomaId;

    /** -------- GEOMETRY FIELDS (same as Ayuntamiento) -------- */

    @JsonIgnore
    @Column(name = "geometry", columnDefinition = "CLOB")
    private String geometryJson;

    @JsonIgnore
    @Transient
    private Geometry geometry;

    public Provincia() {}

    public Provincia(String provinciaId, String name, String comunidadAutonomaId, Geometry geometry) {
        this.provinciaId = provinciaId;
        this.name = name;
        this.comunidadAutonomaId = comunidadAutonomaId;
        this.geometry = geometry;
    }

    public String getId() { return provinciaId; }

    public String getName() { return name; }

    public String getIdealistaLocationId() {
        return String.join("-", "0", "EU", "ES", provinciaId);
    }

    public void setProvinciaId(String provinciaId) { this.provinciaId = provinciaId; }

    public void setName(String name) { this.name = name; }

    public String getComunidadAutonomaId() {
        return comunidadAutonomaId;
    }

    public void setComunidadAutonomaId(String comunidadAutonomaId) {
        this.comunidadAutonomaId = comunidadAutonomaId;
    }

    /* ------------ GEOMETRY GETTERS ------------- */

    public Geometry getGeometry() { return geometry; }

    public String getGeometryJson() { return geometryJson; }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public void setGeometryJson(String geometryJson) {
        this.geometryJson = geometryJson;
        this.geometry = null; // force re-parse
    }

    /** Geometry-derived properties exposed as JSON */

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

    /** ----------- LIFECYCLE HOOKS (same as Ayuntamiento) ---------- */

    @PrePersist
    @PreUpdate
    private void serializeGeometry() {
        if (geometry != null) {
            try {
                org.locationtech.jts.io.geojson.GeoJsonWriter writer =
                        new org.locationtech.jts.io.geojson.GeoJsonWriter();
                this.geometryJson = writer.write(geometry);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize geometry for provincia: " + provinciaId, e);
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
                throw new RuntimeException("Failed to parse geometry for provincia: " + provinciaId, e);
            }
        }
    }

    @Override
    public String toString() {
        return "Provincia{" +
                "provinciaId='" + provinciaId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

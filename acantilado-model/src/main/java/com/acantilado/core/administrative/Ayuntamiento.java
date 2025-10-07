package com.acantilado.core.administrative;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import javax.persistence.*;
import java.util.Map;
import java.util.Objects;


@Entity
@Table(name = "AYUNTAMIENTO")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.acantilado.ayuntamiento.findAll",
                        query = "SELECT a FROM Ayuntamiento a"
                ),
                @NamedQuery(
                        name = "com.acantilado.ayuntamiento.findByProvinceId",
                        query = "SELECT a FROM Ayuntamiento a WHERE a.provincia_id = :provincia_id"
                ),
        }
)
public class Ayuntamiento {
    @Id
    /* The INE code structure works as follows:5-digit format: PPMMM
     * PP = Province code (2 digits)
     * MMM = Municipality code within the province (3 digits)
     */
    @Column(name = "ayuntamiento_id")
    private long ayuntamiento_id;

    @Column(name = "provincia_id")
    private long provincia_id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @JsonIgnore  // Don't expose the raw CLOB in API responses
    @Column(name = "geometry", columnDefinition = "CLOB")
    private String geometryJson;

    @JsonIgnore
    @Transient
    private Geometry geometry;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = Provincia.class)
    @JoinColumn(name = "provincia", referencedColumnName= "provincia_id", nullable = false)
    private Provincia provincia;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "comunidad_autonoma", referencedColumnName= "comunidad_autonoma_id", nullable = false)
    private ComunidadAutonoma comunidadAutonoma;

    public Ayuntamiento() {}

    public Ayuntamiento(long ayuntamiento_id, String name, long provincia_id, Provincia provincia, ComunidadAutonoma comunidadAutonoma, Geometry geometry) {
        this.ayuntamiento_id = ayuntamiento_id;
        this.name = name;
        this.provincia_id = provincia_id;
        this.provincia = provincia;
        this.comunidadAutonoma = comunidadAutonoma;
        this.geometry = geometry;
    }

    public long getId() {
        return ayuntamiento_id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public String getGeometryJson() {
        return geometryJson;
    }

    public long getProvincia_id() { return provincia_id; }

    public void setId(long id) {
        this.ayuntamiento_id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @JsonProperty("bounds")
    public Map<String, Double> getBounds() {
        if (geometry == null) return null;

        Envelope envelope = geometry.getEnvelopeInternal();
        return Map.of(
                "minLon", envelope.getMinX(),
                "maxLon", envelope.getMaxX(),
                "minLat", envelope.getMinY(),
                "maxLat", envelope.getMaxY()
        );
    }

    @JsonProperty("centroid")
    public Map<String, Double> getCentroid() {
        if (geometry == null) return null;

        Point centroid = geometry.getCentroid();
        return Map.of(
                "lon", centroid.getX(),
                "lat", centroid.getY()
        );
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public void setGeometryJson(String geometryJson) {
        this.geometryJson = geometryJson;
        this.geometry = null; // Clear cached geometry so it gets re-parsed
    }

    @PrePersist
    @PreUpdate
    private void serializeGeometry() {
        if (geometry != null) {
            try {

                org.locationtech.jts.io.geojson.GeoJsonWriter writer =
                        new org.locationtech.jts.io.geojson.GeoJsonWriter();
                this.geometryJson = writer.write(geometry);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize geometry", e);
            }
        }
    }

    public void setProvincia_id(long provincia_id) { this.provincia_id = provincia_id; }

    @PostLoad
    private void parseGeometry() {
        if (geometryJson != null && !geometryJson.isEmpty()) {
            try {
                GeoJsonReader reader = new GeoJsonReader();
                this.geometry = reader.read(geometryJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse geometry for ayuntamiento: " + ayuntamiento_id, e);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(ayuntamiento_id, name, phone);
    }

    @Override
    public String toString() {
        return "Ayuntamiento{" +
                "ayuntamiento_id=" + ayuntamiento_id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", provincia=" + provincia +
                ", comunidadAutonoma=" + comunidadAutonoma +
                '}';
    }
}

package com.acantilado.core.administrative;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Entity
@Table(name = "ayuntamiento")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "ayuntamientoId")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.acantilado.ayuntamiento.findAll",
                        query = "SELECT a FROM Ayuntamiento a"
                ),
                @NamedQuery(
                        name = "com.acantilado.ayuntamiento.findByProvinceId",
                        query = "SELECT a FROM Ayuntamiento a WHERE a.provinciaId = :provinciaId"
                ),
                @NamedQuery(
                        name = "com.acantilado.ayuntamiento.findByComunidadAutonomaId",
                        query = "SELECT a FROM Ayuntamiento a WHERE a.comunidadAutonomaId = :comunidadAutonomaId"
                ),
                @NamedQuery(
                        name = "com.acantilado.ayuntamiento.findByName",
                        query = "SELECT a FROM Ayuntamiento a WHERE a.name = :name"
                ),
        }
)
public class Ayuntamiento {
    @Id
    /* The INE code structure works as follows: 5-digit format: PPMMMM
     * PP = Province code (2 digits)
     * MMM = Municipality code within the province (3 digits)
     */
    @JsonProperty("ayuntamientoId")
    @Column(name = "ayuntamientoId")
    private String ayuntamientoId;

    @Column(name = "provinciaId", nullable = false)
    private String provinciaId;

    @Column(name = "comunidadAutonomaId", nullable = false)
    private String comunidadAutonomaId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @JsonIgnore  // Don't expose the raw CLOB in API responses
    @Column(name = "geometry", columnDefinition = "CLOB", nullable = false)
    private String geometryJson;

    @JsonIgnore
    @Transient
    private Geometry geometry;

    @ManyToMany(mappedBy = "ayuntamientos", fetch = FetchType.LAZY)
    private Set<CodigoPostal> codigosPostales = new HashSet<>();

    public Set<CodigoPostal> getCodigosPostales() {
        return codigosPostales;
    }

    public void setCodigosPostales(Set<CodigoPostal> codigosPostales) {
        this.codigosPostales = codigosPostales;
    }

    public Ayuntamiento() {
    }

    public Ayuntamiento(
            String ayuntamientoId,
            String name,
            String provinciaId,
            String comunidadAutonomaId,
            Geometry geometry) {

        this.ayuntamientoId = ayuntamientoId;
        this.name = name;
        this.provinciaId = provinciaId;
        this.comunidadAutonomaId = comunidadAutonomaId;
        this.geometry = geometry;
    }

    public String getId() {
        return ayuntamientoId;
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

    public String getProvinciaId() {
        return provinciaId;
    }

    public String getComunidadAutonomaId() {
        return comunidadAutonomaId;
    }

    public void setId(String id) {
        this.ayuntamientoId = id;
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

    public void setProvinciaId(String provinciaId) {
        this.provinciaId = provinciaId;
    }

    public void setComunidadAutonomaId(String comunidadAutonomaId) {
        this.comunidadAutonomaId = comunidadAutonomaId;
    }

    @PostLoad
    private void parseGeometry() {
        if (geometryJson != null && !geometryJson.isEmpty()) {
            try {
                GeoJsonReader reader = new GeoJsonReader();
                this.geometry = reader.read(geometryJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse geometry for ayuntamiento: " + ayuntamientoId, e);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(ayuntamientoId, name, phone);
    }

    @Override
    public String toString() {
        return "Ayuntamiento{" +
                "ayuntamientoId='" + ayuntamientoId + '\'' +
                ", provinciaId='" + provinciaId + '\'' +
                ", comunidadAutonomaId='" + comunidadAutonomaId + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}


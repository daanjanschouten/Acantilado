package com.acantilado.core.administrative;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Entity
@Table(name = "BARRIO")
@NamedQueries({
        @NamedQuery(
                name = "com.acantilado.barrio.findAll",
                query = "SELECT b FROM Barrio b"
        ),
        @NamedQuery(
                name = "com.acantilado.barrio.findByAyuntamiento",
                query = "SELECT b FROM Barrio b WHERE b.ayuntamientoId = :ayuntamiento_id"
        ),
        @NamedQuery(
                name = "com.acantilado.barrio.findByName",
                query = "SELECT b FROM Barrio b WHERE b.name = :name"
        ),
        @NamedQuery(
                name = "com.acantilado.barrio.findByAyuntamientoAndName",
                query = "SELECT b FROM Barrio b WHERE b.ayuntamientoId = :ayuntamiento_id AND b.name = :name"
        )
})
public class Barrio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "barrio_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    // City/municipality this barrio belongs to
    @Column(name = "ayuntamiento_id", nullable = false)
    private Long ayuntamientoId;

    @JsonIgnore
    @Column(name = "geometry", columnDefinition = "CLOB", nullable = false)
    private String geometryJson;

    @Transient
    @JsonIgnore
    private Geometry geometry;

    // Many-to-one with Ayuntamiento (a barrio belongs to one city)
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ayuntamiento_id", referencedColumnName = "ayuntamiento_id",
            insertable = false, updatable = false)
    private Ayuntamiento ayuntamiento;

    // Many-to-many with CodigoPostal
    @JsonIgnore
    @ManyToMany(mappedBy = "barrios")
    private Set<CodigoPostal> codigosPostales = new HashSet<>();

    public Barrio() {}

    public Barrio(String name, Long ayuntamientoId, Geometry geometry) {
        this.name = name;
        this.ayuntamientoId = ayuntamientoId;
        this.geometry = geometry;
    }

    @PostLoad
    private void parseGeometry() {
        if (geometryJson != null && !geometryJson.isEmpty()) {
            try {
                GeoJsonReader reader = new GeoJsonReader();
                this.geometry = reader.read(geometryJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse geometry for barrio: " + id, e);
            }
        }
    }

    @PrePersist
    @PreUpdate
    private void serializeGeometry() {
        if (geometry != null) {
            try {
                GeoJsonWriter writer = new GeoJsonWriter();
                this.geometryJson = writer.write(geometry);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize geometry for barrio: " + id, e);
            }
        }
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAyuntamientoId() {
        return ayuntamientoId;
    }

    public void setAyuntamientoId(Long ayuntamientoId) {
        this.ayuntamientoId = ayuntamientoId;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public String getGeometryJson() {
        return geometryJson;
    }

    public void setGeometryJson(String geometryJson) {
        this.geometryJson = geometryJson;
        this.geometry = null;
    }

    public Ayuntamiento getAyuntamiento() {
        return ayuntamiento;
    }

    public void setAyuntamiento(Ayuntamiento ayuntamiento) {
        this.ayuntamiento = ayuntamiento;
    }

    public Set<CodigoPostal> getCodigosPostales() {
        return codigosPostales;
    }

    public void setCodigosPostales(Set<CodigoPostal> codigosPostales) {
        this.codigosPostales = codigosPostales;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Barrio barrio = (Barrio) o;
        // Use business key: name + ayuntamiento
        return Objects.equals(name, barrio.name) &&
                Objects.equals(ayuntamientoId, barrio.ayuntamientoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ayuntamientoId);
    }

    @Override
    public String toString() {
        return "Barrio{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ayuntamientoId=" + ayuntamientoId +
                ", hasGeometry=" + (geometry != null) +
                '}';
    }
}
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
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "codigo_postal")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "codigoIne")
@NamedQueries({
  @NamedQuery(name = "com.acantilado.codigopostal.findAll", query = "SELECT c FROM CodigoPostal c"),
  @NamedQuery(
      name = "com.acantilado.codigopostal.findByAyuntamiento",
      query =
          "SELECT c FROM CodigoPostal c JOIN c.ayuntamientos a WHERE a.ayuntamientoId = :ayuntamientoId"),
  @NamedQuery(
      name = "com.acantilado.codigopostal.findByCodigoPostal",
      query = "SELECT c FROM CodigoPostal c WHERE c.codigoPostal = :codigo_postal")
})
public class CodigoPostal {

  @Id
  @Column(name = "codigo_ine")
  @JsonProperty("codigoIne")
  private String codigoIne;

  @Column(name = "codigo_postal", length = 5, nullable = false)
  private String codigoPostal;

  @JsonIgnore
  @Column(name = "geometry", columnDefinition = "CLOB", nullable = false)
  private String geometryJson;

  @Transient @JsonIgnore private Geometry geometry;

  @ManyToMany
  @JoinTable(
      name = "CODIGO_POSTAL_AYUNTAMIENTO",
      joinColumns = @JoinColumn(name = "codigo_ine"),
      inverseJoinColumns = @JoinColumn(name = "ayuntamientoId"))
  private Set<Ayuntamiento> ayuntamientos = new HashSet<>();

  public CodigoPostal() {}

  public CodigoPostal(String codigoIne, String codigoPostal, Geometry geometry) {
    this.codigoIne = codigoIne;
    this.codigoPostal = codigoPostal;
    this.geometry = geometry;
  }

  @PostLoad
  private void parseGeometry() {
    if (geometryJson != null && !geometryJson.isEmpty()) {
      try {
        GeoJsonReader reader = new GeoJsonReader();
        this.geometry = reader.read(geometryJson);
      } catch (Exception e) {
        throw new RuntimeException(
            "Failed to parse geometry for codigo postal: " + codigoPostal, e);
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
        throw new RuntimeException(
            "Failed to serialize geometry for codigo postal: " + codigoPostal, e);
      }
    }
  }

  // Getters and setters
  public String getCodigoIne() {
    return codigoIne;
  }

  public void setCodigoIne(String codigoIne) {
    this.codigoIne = codigoIne;
  }

  public String getCodigoPostal() {
    return codigoPostal;
  }

  public void setCodigoPostal(String codigoPostal) {
    this.codigoPostal = codigoPostal;
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

  public Set<Ayuntamiento> getAyuntamientos() {
    return ayuntamientos;
  }

  public void setAyuntamientos(Set<Ayuntamiento> ayuntamientos) {
    this.ayuntamientos = ayuntamientos;
  }

  @JsonProperty("bounds")
  public Map<String, Double> getBounds() {
    if (geometry == null) return null;
    Envelope envelope = geometry.getEnvelopeInternal();
    return Map.of(
        "minLon", envelope.getMinX(),
        "maxLon", envelope.getMaxX(),
        "minLat", envelope.getMinY(),
        "maxLat", envelope.getMaxY());
  }

  @JsonProperty("centroid")
  public Map<String, Double> getCentroid() {
    if (geometry == null) return null;
    Point centroid = geometry.getCentroid();
    return Map.of(
        "lon", centroid.getX(),
        "lat", centroid.getY());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    CodigoPostal that = (CodigoPostal) o;
    return Objects.equals(codigoIne, that.codigoIne)
        && Objects.equals(codigoPostal, that.codigoPostal)
        && Objects.equals(geometryJson, that.geometryJson)
        && Objects.equals(geometry, that.geometry)
        && Objects.equals(ayuntamientos, that.ayuntamientos);
  }

  @Override
  public int hashCode() {
    return Objects.hash(codigoIne, codigoPostal, geometryJson, geometry, ayuntamientos);
  }

  @Override
  public String toString() {
    return "CodigoPostal{"
        + "codigoIne='"
        + codigoIne
        + '\''
        + ", codigoPostal='"
        + codigoPostal
        + '}';
  }
}

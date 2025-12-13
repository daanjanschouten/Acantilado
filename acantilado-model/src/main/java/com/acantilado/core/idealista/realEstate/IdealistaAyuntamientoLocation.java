package com.acantilado.core.idealista.realEstate;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "idealista_ayuntamiento_location")
@NamedQueries({
  @NamedQuery(
      name =
          "com.schouten.core.properties.idealista.IdealistaAyuntamientoLocation.findByProvinciaId",
      query = "SELECT p FROM IdealistaAyuntamientoLocation p WHERE p.provinciaId = :provinciaId"),
  @NamedQuery(
      name = "com.schouten.core.properties.idealista.IdealistaAyuntamientoLocation.findByLocation",
      query =
          "SELECT p FROM IdealistaAyuntamientoLocation p WHERE p.ayuntamientoLocationId = :ayuntamientoLocationId")
})
public class IdealistaAyuntamientoLocation {

  @Id
  @Column(name = "idealista_ayuntamiento_location_id")
  private String ayuntamientoLocationId;

  @Column(name = "provinciaId")
  private String provinciaId;

  public IdealistaAyuntamientoLocation(String locationId) {
    this.ayuntamientoLocationId = getAyuntamientoLocationId(locationId);
    this.provinciaId = getProvinciaId(locationId);
  }

  public IdealistaAyuntamientoLocation() {}

  public String getAyuntamientoLocationId() {
    return ayuntamientoLocationId;
  }

  public void setAyuntamientoLocationId(String ayuntamientoLocationId) {
    this.ayuntamientoLocationId = ayuntamientoLocationId;
  }

  public String getProvinciaId() {
    return provinciaId;
  }

  public void setProvinciaId(String provinciaId) {
    this.provinciaId = provinciaId;
  }

  private String getAyuntamientoLocationId(String idealistaLocationId) {
    String[] parts = idealistaLocationId.split("-");
    if (parts.length <= 7) {
      return idealistaLocationId;
    }

    return String.join("-", Arrays.copyOfRange(parts, 0, 7));
  }

  private String getProvinciaId(String idealistaLocationId) {
    String[] parts = idealistaLocationId.split("-");
    if (parts.length <= 4) {
      return idealistaLocationId;
    }

    return String.join("-", Arrays.copyOfRange(parts, 0, 4));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IdealistaAyuntamientoLocation that = (IdealistaAyuntamientoLocation) o;
    return Objects.equals(ayuntamientoLocationId, that.ayuntamientoLocationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ayuntamientoLocationId);
  }

  @Override
  public String toString() {
    return "IdealistaAyuntamientoLocation{"
        + "ayuntamientoLocationId='"
        + ayuntamientoLocationId
        + '\''
        + ", provinciaId='"
        + provinciaId
        + '\''
        + '}';
  }
}

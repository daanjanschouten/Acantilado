package com.acantilado.core.administrative;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "idealista_location_mapping")
@NamedQueries({
        @NamedQuery(
                name = "com.acantilado.core.administrative.IdealistaLocationMapping.findAll",
                query = "SELECT m FROM IdealistaLocationMapping m"
        ),
        @NamedQuery(
                name = "com.acantilado.core.administrative.IdealistaLocationMapping.findByAyuntamientoId",
                query = "SELECT m FROM IdealistaLocationMapping m WHERE m.acantiladoAyuntamientoId = :ayuntamientoId"
        ),
        @NamedQuery(
                name = "com.acantilado.core.administrative.IdealistaLocationMapping.findByIdealistaLocationId",
                query = "SELECT m FROM IdealistaLocationMapping m WHERE m.idealistaLocationId = :idealistaLocationId"
        ),
        @NamedQuery(
                name = "com.acantilado.core.administrative.IdealistaLocationMapping.findByIdealistaMunicipalityName",
                query = "SELECT m FROM IdealistaLocationMapping m WHERE m.idealistaMunicipalityName = :municipalityName"
        )
})
public class IdealistaLocationMapping implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "idealista_location_id", nullable = false)
    private String idealistaLocationId;

    @Column(name = "idealista_municipality_name", nullable = false)
    private String idealistaMunicipalityName;

    @Column(name = "acantilado_ayuntamiento_id", nullable = false)
    private Long acantiladoAyuntamientoId;

    @Column(name = "acantilado_municipality_name", nullable = false)
    private String acantiladoMunicipalityName;

    @Transient
    private Integer confidenceScore;

    // Constructors
    public IdealistaLocationMapping() {
    }

    public IdealistaLocationMapping(String idealistaLocationId,
                                    String idealistaMunicipalityName,
                                    Long acantiladoAyuntamientoId,
                                    String acantiladoMunicipalityName) {
        this.idealistaLocationId = idealistaLocationId;
        this.idealistaMunicipalityName = idealistaMunicipalityName;
        this.acantiladoAyuntamientoId = acantiladoAyuntamientoId;
        this.acantiladoMunicipalityName = acantiladoMunicipalityName;

        this.confidenceScore = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdealistaLocationId() { return idealistaLocationId; }
    public void setIdealistaLocationId(String idealistaLocationId) { this.idealistaLocationId = idealistaLocationId; }

    public Integer getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Integer confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getIdealistaMunicipalityName() { return idealistaMunicipalityName; }
    public void setIdealistaMunicipalityName(String idealistaMunicipalityName) { this.idealistaMunicipalityName = idealistaMunicipalityName; }

    public Long getAcantiladoAyuntamientoId() { return acantiladoAyuntamientoId; }
    public void setAcantiladoAyuntamientoId(Long acantiladoAyuntamientoId) { this.acantiladoAyuntamientoId = acantiladoAyuntamientoId; }

    public String getAcantiladoMunicipalityName() { return acantiladoMunicipalityName; }
    public void setAcantiladoMunicipalityName(String acantiladoMunicipalityName) { this.acantiladoMunicipalityName = acantiladoMunicipalityName; }

    @Override
    public String toString() {
        return "IdealistaLocationMapping{" +
                "id=" + id +
                ", idealistaLocationId='" + idealistaLocationId + '\'' +
                ", idealistaMunicipalityName='" + idealistaMunicipalityName + '\'' +
                ", acantiladoAyuntamientoId=" + acantiladoAyuntamientoId +
                ", acantiladoMunicipalityName='" + acantiladoMunicipalityName + '\'' +
                ", confidenceScore=" + confidenceScore +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdealistaLocationMapping that = (IdealistaLocationMapping) o;
        return Objects.equals(idealistaLocationId, that.idealistaLocationId) && Objects.equals(idealistaMunicipalityName, that.idealistaMunicipalityName) && Objects.equals(acantiladoAyuntamientoId, that.acantiladoAyuntamientoId) && Objects.equals(acantiladoMunicipalityName, that.acantiladoMunicipalityName) && Objects.equals(confidenceScore, that.confidenceScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idealistaLocationId, idealistaMunicipalityName, acantiladoAyuntamientoId, acantiladoMunicipalityName, confidenceScore);
    }
}
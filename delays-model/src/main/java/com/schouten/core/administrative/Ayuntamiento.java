package com.schouten.core.administrative;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "AYUNTAMIENTO")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.ayuntamiento.findAll",
                        query = "SELECT a FROM Ayuntamiento a"
                )
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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = com.schouten.core.administrative.Provincia.class)
    @JoinColumn(name = "provincia", referencedColumnName= "provincia_id", nullable = false)
    private Provincia provincia;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "comunidad_autonoma", referencedColumnName= "comunidad_autonoma_id", nullable = false)
    private ComunidadAutonoma comunidadAutonoma;

    // Default constructor that Hibernate requires.
    public Ayuntamiento() {}

    public Ayuntamiento(long ayuntamiento_id, String name, Provincia provincia, ComunidadAutonoma comunidadAutonoma) {
        this.ayuntamiento_id = ayuntamiento_id;
        this.name = name;
        this.provincia = provincia;
        this.comunidadAutonoma = comunidadAutonoma;
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

    public void setId(long id) {
        this.ayuntamiento_id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

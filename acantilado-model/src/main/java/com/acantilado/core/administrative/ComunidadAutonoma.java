package com.acantilado.core.administrative;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "COMUNIDAD_AUTONOMA")
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
    @Column(name = "comunidad_autonoma_id")
    private long comunidad_autonoma_id;

    @Column(name = "name", nullable = false)
    private String name;

    public ComunidadAutonoma() {}

    public ComunidadAutonoma(long comunidad_autonoma_id, String name) {
        this.comunidad_autonoma_id = comunidad_autonoma_id;
        this.name = name;
    }

    public long getId() {
        return comunidad_autonoma_id;
    }

    public String getName() {
        return name;
    }

    public void setId(long id) {
        this.comunidad_autonoma_id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(comunidad_autonoma_id, name);
    }

    @Override
    public String toString() {
        return "ComunidadAutonoma{" +
                "comunidad_autonoma_id=" + comunidad_autonoma_id +
                ", name='" + name + '\'' +
                '}';
    }
}

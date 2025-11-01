package com.acantilado.core.administrative;

import javax.persistence.*;

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
    @Column(name = "provincia_id")
    private long provincia_id;

    @Column(name = "name", nullable = false)
    private String name;

    public Provincia() {}

    public Provincia(long provincia_id, String name) {
        this.provincia_id = provincia_id;
        this.name = name;
    }

    public long getId() {
        return provincia_id;
    }

    public String getName() {
        return name;
    }

    public String getIdealistaLocationId() {
        return String.format("0-EU-ES-%02d", provincia_id);
    }

    public void setProvincia_id(long provincia_id) {
        this.provincia_id = provincia_id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Provincia{" +
                "provincia_id=" + provincia_id +
                ", name='" + name + '\'' +
                '}';
    }
}


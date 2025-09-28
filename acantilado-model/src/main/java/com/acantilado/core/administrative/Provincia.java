package com.acantilado.core.administrative;

import javax.persistence.*;

@Entity
@Table(name = "PROVINCIA")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.provincia.findAll",
                        query = "SELECT p FROM Provincia p"
                )
        }
)
public class Provincia {
    @Id
    @Column(name = "provincia_id")
    private long provincia_id;

    @Column(name = "name", nullable = false)
    private String name;

    // Default constructor (required by Hibernate)
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


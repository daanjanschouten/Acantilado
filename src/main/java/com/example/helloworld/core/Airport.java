package com.example.helloworld.core;

import javax.persistence.*;
@Table(name = "airports")
@Entity
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long airport_id;
    private Location location;
    @Column(name = "name", nullable = false)
    private String name;

    public Airport(Location location, String name) {
        this.location = location;
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return airport_id;
    }

    public void setId(long airport_id) {
        this.airport_id = airport_id;
    }
}

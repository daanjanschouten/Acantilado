package com.example.helloworld.core;

import javax.persistence.*;

@Entity
@Table(name = "AIRPORT")
public class Runway {
    @Id
    @Column(name = "runway_id")
    private long runwayId;
    @Column(name = "country", nullable = false)
    private String country;
    @Column(name = "city", nullable = false)
    private String city;
    @Column(name = "name", nullable = false)
    private String name;

    public Runway() {
    }

    public Runway(long airportId, String country, String city, String name) {
        this.runwayId = airportId;
        this.country = country;
        this.city = city;
        this.name = name;
    }

    public long getAirportId() {
        return runwayId;
    }

    public void setAirportId(long airportId) {
        this.runwayId = airportId;
    }
}

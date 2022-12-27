package com.schouten.core.aviation;

import javax.persistence.*;

@Entity
@Table(name = "AIRPORT")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.flightdelays.core.Airport.findAll",
                        query = "SELECT a FROM Airport a"
                )
        }
)
public class Airport {
    @Id
    @Column(name = "airport_id")
    private String airportId;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "country", nullable = false)
    private String country;
    @Column(name = "city", nullable = false)
    private String city;

    public Airport() {
    }

    public Airport(String airportId, String name, String country, String city) {
        this.airportId = airportId;
        this.name = name;
        this.country = country;
        this.city = city;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAirportId() {
        return airportId;
    }

    public void setAirportId(String airportId) {
        this.airportId = airportId;
    }
}

package com.schouten.core.aviation;

import javax.persistence.*;
import java.util.Objects;

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
    @Column(name = "iata_id")
    private String iataId;
    @Column(name = "name")
    private String name;
    @Column(name = "country", nullable = false)
    private String country;
    @Column(name = "latitude", nullable = false)
    private double latitude;
    @Column(name = "longitude", nullable = false)
    private double longitude;

    public Airport() {
    }

    public Airport(String iataId, String name, String country, double latitude, double longitude) {
        this.iataId = iataId;
        this.name = name;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public String getAirportId() {
        return iataId;
    }

    public void setAirportId(String airportId) {
        this.iataId = airportId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "Airport{" +
                "iataId='" + iataId + '\'' +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Airport airport = (Airport) o;
        return Double.compare(airport.latitude, latitude) == 0 && Double.compare(airport.longitude, longitude) == 0 && iataId.equals(airport.iataId) && name.equals(airport.name) && country.equals(airport.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iataId, name, country, latitude, longitude);
    }
}

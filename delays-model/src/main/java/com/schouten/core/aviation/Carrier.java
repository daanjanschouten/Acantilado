package com.schouten.core.aviation;

import javax.persistence.*;

@Entity
@Table(name = "CARRIER")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.flightdelays.core.Carrier.findAll",
                        query = "SELECT c FROM Carrier c"
                )
        }
)
public class Carrier {
    @Id
    @Column(name="iata_id")
    private String iataId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "hub", nullable = false)
    private String hub;

    @Column(name = "country", nullable = false)
    private String country;

    public Carrier(String iataId, String name, String hub, String country) {
        this.iataId = iataId;
        this.name = name;
        this.hub = hub;
        this.country = country;
    }

    public Carrier() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIataId() {
        return iataId;
    }

    public void setIataId(String iataId) {
        this.iataId = iataId;
    }

    public String getHub() {
        return hub;
    }

    public void setHub(String hub) {
        this.hub = hub;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return "Carrier{" +
                "iataId='" + iataId + '\'' +
                ", name='" + name + '\'' +
                ", hub='" + hub + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}

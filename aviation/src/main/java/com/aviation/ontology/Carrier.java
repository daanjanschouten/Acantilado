package com.aviation.ontology;

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
    @Column(name="carrier_id")
    private String carrierId;

    @Column(name = "name", nullable = false)
    private String name;

    public Carrier() {
    }

    public Carrier(String carrierId, String name) {
        this.carrierId = carrierId;
        this.name = name;
    }

    public String getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(String carrierId) {
        this.carrierId = carrierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package com.flightdelays.aviation.ontology;

import javax.persistence.*;

@Entity
@Table(name = "CARRIER")
public class Carrier {
    @Id
    @Column(name="carrier_id")
    private long carrierId;

    @Column(name = "name", nullable = false)
    private String name;

    public Carrier() {
    }

    public Carrier(long carrierId, String name) {
        this.carrierId = carrierId;
        this.name = name;
    }

    public long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(long carrierId) {
        this.carrierId = carrierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package com.example.helloworld.core;

import javax.persistence.*;

@Table(name = "carriers")
@Entity
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long carrier_id;
    @Column(name = "name", nullable = false)
    private String name;

    public Carrier(String name) {
        this.name = name;
    }

    public long getId() {
        return carrier_id;
    }

    public void setId(long carrier_id) {
        this.carrier_id = carrier_id;
    }
}

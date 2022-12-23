package com.example.helloworld.core;

import com.example.helloworld.core.schedules.CarrierSchedule;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CARRIER")
public class Carrier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="carrier_id")
    private long carrierId;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "carrier")
    private Set<CarrierSchedule> carrierSchedules = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "carrier")
    private Set<CarrierSchedule> oepratorSchedules = new HashSet<>();

    @Column(name = "name", nullable = false)
    private String name;

    public Carrier(String name) {
        this.name = name;
    }
}

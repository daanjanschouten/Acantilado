package com.example.helloworld.core;

import com.example.helloworld.core.schedules.AircraftSchedule;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "AIRCRAFT")
public class Aircraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="aircraft_id")
    private long aircraftId;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "scheduledAircraft", cascade = CascadeType.ALL)
    private Set<AircraftSchedule> scheduledAircraftSchedules = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "actualAircraft", cascade = CascadeType.ALL)
    private Set<AircraftSchedule> actualAircraftSchedules = new HashSet<>();

    public Aircraft() {
    }
}

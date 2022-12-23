package com.example.helloworld.core;

import com.example.helloworld.core.schedules.AirportSchedule;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "AIRPORT")
public class Airport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "airport_id")
    private long airportId;

    @OneToOne(mappedBy = "airport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Location location;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "scheduledDepartureAirport")
    private Set<AirportSchedule> airportScheduledDepartureSchedules = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "scheduledArrivalAirport")
    private Set<AirportSchedule> airportScheduledArrivalSchedules = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "actualDepartureAirport")
    private Set<AirportSchedule> airportActualDepartureSchedules = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "actualArrivalAirport")
    private Set<AirportSchedule> airportActualArrivalSchedules = new HashSet<>();

    public Airport(String name) {
        this.name = name;
    }
}

package com.example.helloworld.core.schedules;

import com.example.helloworld.core.Aircraft;
import com.example.helloworld.core.Flight;

import javax.persistence.*;

@Entity
@Table(name = "AIRCRAFT_SCHEDULE")
public class AircraftSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aircraft_schedule_id")
    private long aircraftScheduleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id", updatable = false, insertable = false)
    private Aircraft scheduledAircraft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id", updatable = false, insertable = false)
    private Aircraft actualAircraft;

    public AircraftSchedule() {
    }
}

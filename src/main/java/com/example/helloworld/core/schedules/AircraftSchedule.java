package com.example.helloworld.core.schedules;

import com.example.helloworld.core.Flight;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "AIRCRAFT_SCHEDULE")
public class AircraftSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aircraftScheduleId")
    private UUID aircraftScheduleId;
//    @Column(name = "scheduledAircraft", nullable = false)
//    private Aircraft scheduledAircraft;
//    @Column(name = "actualAircraft", nullable = false)
//    private Aircraft actualAircraft;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    public AircraftSchedule() {
    }
}

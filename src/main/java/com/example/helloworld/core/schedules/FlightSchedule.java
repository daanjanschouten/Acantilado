package com.example.helloworld.core.schedules;

import com.example.helloworld.core.Flight;

import javax.persistence.*;
import java.util.UUID;
//import java.time.Instant;


@Entity
@Table(name = "FLIGHT_SCHEDULE")
public class FlightSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flightScheduleId")
    private UUID flightScheduleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

//    @Column(name = "scheduledDepature", nullable = false)
//    private Instant scheduledDeparture;
//    @Column(name = "actualDepature", nullable = false)
//    private Instant scheduledArrival;
//    @Column(name = "scheduledArrival", nullable = false)
//    private Instant actualDeparture;
//    @Column(name = "actualArrival", nullable = false)
//    private Instant actualArrival;
//
//    public FlightSchedule(Instant scheduledDeparture, Instant scheduledArrival, Instant actualDeparture, Instant actualArrival) {
//        this.scheduledDeparture = scheduledDeparture;
//        this.scheduledArrival = scheduledArrival;
//        this.actualDeparture = actualDeparture;
//        this.actualArrival = actualArrival;
//    }

    public FlightSchedule() {
    }
}

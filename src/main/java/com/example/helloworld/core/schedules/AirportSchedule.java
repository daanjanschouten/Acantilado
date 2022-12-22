package com.example.helloworld.core.schedules;

import com.example.helloworld.core.Airport;
import com.example.helloworld.core.Flight;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "AIRPORT_SCHEDULE")
public class AirportSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "airport_schedule_id")
    private UUID airportScheduleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airport_id", updatable = false, insertable = false)
    private Airport scheduledDepartureAirport;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airport_id", updatable = false, insertable = false)
    private Airport scheduledArrivalAirport;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airport_id", updatable = false, insertable = false)
    private Airport actualDepartureAirport;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airport_id", updatable = false, insertable = false)
    private Airport actualArrivalAirport;

    public AirportSchedule() {
    }
}

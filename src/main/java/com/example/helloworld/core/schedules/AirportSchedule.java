package com.example.helloworld.core.schedules;

import com.example.helloworld.core.Flight;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "AIRPORT_SCHEDULE")
public class AirportSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flightScheduleId")
    private UUID airportScheduleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

//    @ManyToOne
//    @JoinColumn(name= "id")
//    @Column(name = "scheduledDepartureAirport", nullable = false)
//    private Airport scheduledDepartureAirport;
//    @ManyToOne
//    @JoinColumn(name= "id")
//    @Column(name = "actualDepartureAirport", nullable = false)
//    private Airport actualDepartureAirport;
//    @ManyToOne
//    @JoinColumn(name= "id")
//    @Column(name = "scheduledArrivalAirport", nullable = false)
//    private Airport scheduledArrivalAirport;
//    @ManyToOne
//    @JoinColumn(name= "id")
//    @Column(name = "actualArrivalAirport", nullable = false)
//    private Airport actualArrivalAirport;


    public AirportSchedule() {
    }
}

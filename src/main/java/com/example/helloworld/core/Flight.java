package com.example.helloworld.core;

//import com.example.helloworld.core.schedules.AircraftSchedule;
//import com.example.helloworld.core.schedules.AirportSchedule;
//import com.example.helloworld.core.schedules.CarrierSchedule;
import com.example.helloworld.core.schedules.AircraftSchedule;
import com.example.helloworld.core.schedules.AirportSchedule;
import com.example.helloworld.core.schedules.CarrierSchedule;
import com.example.helloworld.core.schedules.TimeSchedule;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "FLIGHT")
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_id")
    private UUID flightId;
    @OneToOne(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AircraftSchedule aircraftSchedule;
    @OneToOne(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AirportSchedule airportSchedule;
    @OneToOne(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CarrierSchedule carrierSchedule;
    @OneToOne(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TimeSchedule timeSchedule;
    public Flight() {
    }
}

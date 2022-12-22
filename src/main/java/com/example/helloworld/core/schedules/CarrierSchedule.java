package com.example.helloworld.core.schedules;

import com.example.helloworld.core.Flight;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "CARRIER_SCHEDULE")
public class CarrierSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flightScheduleId")
    private UUID carrierScheduleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;
//    @ManyToOne
//    @JoinColumn(name= "id")
//    @Column(name = "carrier", nullable = false)
//    private Carrier carrier;
//    @ManyToOne
//    @JoinColumn(name= "id")
//    @Column(name = "operator", nullable = false)
//    private Carrier operator;

    public CarrierSchedule() {
    }
}

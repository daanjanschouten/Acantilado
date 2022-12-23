package com.example.helloworld.core.schedules;

import com.example.helloworld.core.Carrier;
import com.example.helloworld.core.Flight;

import javax.persistence.*;

@Entity
@Table(name = "CARRIER_SCHEDULE")
public class CarrierSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carrier_schedule_id")
    private long carrierScheduleId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", insertable = false, updatable = false)
    private Carrier carrier;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", insertable = false, updatable = false)
    private Carrier operator;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    public CarrierSchedule() {
    }
}

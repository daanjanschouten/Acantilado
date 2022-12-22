package com.example.helloworld.core.schedules;

import com.example.helloworld.core.Flight;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "FLIGHT_SCHEDULE")
public class TimeSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_schedule_id")
    private UUID timeScheduleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @Column(name = "scheduledDepature", nullable = false)
    private Instant scheduledDeparture;
    @Column(name = "actualDepature", nullable = false)
    private Instant scheduledArrival;
    @Column(name = "scheduledArrival", nullable = false)
    private Instant actualDeparture;
    @Column(name = "actualArrival", nullable = false)
    private Instant actualArrival;

    public TimeSchedule() {
    }
}

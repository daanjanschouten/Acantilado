package com.example.helloworld.core.schedules;

import com.example.helloworld.core.Flight;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "FLIGHT_SCHEDULE")
public class TimeSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_schedule_id")
    private long timeScheduleId;

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

    public TimeSchedule(Instant scheduledDeparture, Instant scheduledArrival, Instant actualDeparture, Instant actualArrival) {
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
        this.actualDeparture = actualDeparture;
        this.actualArrival = actualArrival;
    }

    public TimeSchedule() {
    }

    public Instant getScheduledDeparture() {
        return scheduledDeparture;
    }

    public Instant getScheduledArrival() {
        return scheduledArrival;
    }

    public Instant getActualDeparture() {
        return actualDeparture;
    }

    public Instant getActualArrival() {
        return actualArrival;
    }

    public void setScheduledDeparture(Instant scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public void setScheduledArrival(Instant scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
    }

    public void setActualDeparture(Instant actualDeparture) {
        this.actualDeparture = actualDeparture;
    }

    public void setActualArrival(Instant actualArrival) {
        this.actualArrival = actualArrival;
    }
}

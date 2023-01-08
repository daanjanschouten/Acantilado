package com.schouten.core.aviation;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "FLIGHT")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.example.helloworld.core.Flight.findAll",
                        query = "SELECT f FROM Flight f"
                )
        }
)
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_id")
    private long flightId;

    @Column(name = "flight_number")
    private String flightNumber;
    @Column(name = "scheduled_departure")
    private Instant scheduledDeparture;
    @Column(name = "scheduled_arrival")
    private Instant scheduledArrival;
    @Column(name = "actual_departure")
    private Instant actualDeparture;
    @Column(name = "actual_arrival")
    private Instant actualArrival;

    // Linked fields
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "runway_scheduled_departure", referencedColumnName= "runway_id")
    private Runway scheduledDepartureRunway;
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "runway_scheduled_arrival", referencedColumnName= "runway_id")
    private Runway scheduledArrivalRunway;
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "runway_actual_departure", referencedColumnName= "runway_id")
    private Runway actualDepartureRunway;
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "runway_actual_arrival", referencedColumnName= "runway_id")
    private Runway actualArrivalRunway;
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "carrier", referencedColumnName= "carrier_id")
    private Carrier carrier;
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "operator", referencedColumnName= "carrier_id")
    private Carrier operator;

    public Flight() {
    }

    public Flight(String flightNumber, Instant scheduledDeparture, Instant scheduledArrival, Instant actualDeparture, Instant actualArrival, Aircraft aircraft, Runway scheduledDepartureRunway, Runway scheduledArrivalRunway, Runway actualDepartureRunway, Runway actualArrivalRunway, Carrier carrier, Carrier operator) {
        this.flightNumber = flightNumber;
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
        this.actualDeparture = actualDeparture;
        this.actualArrival = actualArrival;
        this.aircraft = aircraft;
        this.scheduledDepartureRunway = scheduledDepartureRunway;
        this.scheduledArrivalRunway = scheduledArrivalRunway;
        this.actualDepartureRunway = actualDepartureRunway;
        this.actualArrivalRunway = actualArrivalRunway;
        this.carrier = carrier;
        this.operator = operator;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public Instant getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(Instant scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public Instant getScheduledArrival() {
        return scheduledArrival;
    }

    public void setScheduledArrival(Instant scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
    }

    public Instant getActualDeparture() {
        return actualDeparture;
    }

    public void setActualDeparture(Instant actualDeparture) {
        this.actualDeparture = actualDeparture;
    }

    public Instant getActualArrival() {
        return actualArrival;
    }

    public void setActualArrival(Instant actualArrival) {
        this.actualArrival = actualArrival;
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
    }

    public Runway getScheduledDepartureRunway() {
        return scheduledDepartureRunway;
    }

    public void setScheduledDepartureRunway(Runway scheduledDepartureRunway) {
        this.scheduledDepartureRunway = scheduledDepartureRunway;
    }

    public Runway getScheduledArrivalRunway() {
        return scheduledArrivalRunway;
    }

    public void setScheduledArrivalRunway(Runway scheduledArrivalRunway) {
        this.scheduledArrivalRunway = scheduledArrivalRunway;
    }

    public Runway getActualDepartureRunway() {
        return actualDepartureRunway;
    }

    public void setActualDepartureRunway(Runway actualDepartureRunway) {
        this.actualDepartureRunway = actualDepartureRunway;
    }

    public Runway getActualArrivalRunway() {
        return actualArrivalRunway;
    }

    public void setActualArrivalRunway(Runway actualArrivalRunway) {
        this.actualArrivalRunway = actualArrivalRunway;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public Carrier getOperator() {
        return operator;
    }

    public void setOperator(Carrier operator) {
        this.operator = operator;
    }
}

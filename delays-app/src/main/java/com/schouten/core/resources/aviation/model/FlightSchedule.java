package com.schouten.core.resources.aviation.model;

public class FlightSchedule {
    private String scheduledDeparture;
    private String scheduledArrival;
    private String actualDeparture;
    private String actualArrival;

    public FlightSchedule(String scheduledDeparture, String scheduledArrival, String actualDeparture, String actualArrival) {
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
        this.actualDeparture = actualDeparture;
        this.actualArrival = actualArrival;
    }

    public FlightSchedule() {
    }

    public String getScheduledDeparture() {
        return scheduledDeparture;
    }

    public String getScheduledArrival() {
        return scheduledArrival;
    }

    public String getActualDeparture() {
        return actualDeparture;
    }

    public String getActualArrival() {
        return actualArrival;
    }
}

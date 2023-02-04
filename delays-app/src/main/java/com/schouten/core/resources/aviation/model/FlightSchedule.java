package com.schouten.core.resources.aviation.model;

import javax.validation.constraints.NotBlank;

public class FlightSchedule {
    @NotBlank
    private String scheduledDeparture;
    @NotBlank
    private String scheduledArrival;
    @NotBlank
    private String actualDeparture;
    @NotBlank
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

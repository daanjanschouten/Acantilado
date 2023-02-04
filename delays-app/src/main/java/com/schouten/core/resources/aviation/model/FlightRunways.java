package com.schouten.core.resources.aviation.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class FlightRunways {
    @Valid @NotNull private RunwayDetails scheduledDepartureRunway;
    @Valid @NotNull private RunwayDetails scheduledArrivalRunway;
    @Valid @NotNull private RunwayDetails actualDepartureRunway;
    @Valid @NotNull private RunwayDetails actualArrivalRunway;

    public FlightRunways(RunwayDetails scheduledDepartureRunway, RunwayDetails scheduledArrivalRunway, RunwayDetails actualDepartureRunway, RunwayDetails actualArrivalRunway) {
        this.scheduledDepartureRunway = scheduledDepartureRunway;
        this.scheduledArrivalRunway = scheduledArrivalRunway;
        this.actualDepartureRunway = actualDepartureRunway;
        this.actualArrivalRunway = actualArrivalRunway;
    }

    public FlightRunways() {
    }

    public RunwayDetails getScheduledDepartureRunway() {
        return scheduledDepartureRunway;
    }

    public RunwayDetails getScheduledArrivalRunway() {
        return scheduledArrivalRunway;
    }

    public RunwayDetails getActualDepartureRunway() {
        return actualDepartureRunway;
    }

    public RunwayDetails getActualArrivalRunway() {
        return actualArrivalRunway;
    }

    @Override
    public String toString() {
        return "FlightRunways{" +
                "scheduledDepartureRunway=" + scheduledDepartureRunway +
                ", scheduledArrivalRunway=" + scheduledArrivalRunway +
                ", actualDepartureRunway=" + actualDepartureRunway +
                ", actualArrivalRunway=" + actualArrivalRunway +
                '}';
    }
}

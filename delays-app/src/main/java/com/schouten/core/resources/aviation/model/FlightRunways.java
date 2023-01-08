package com.schouten.core.resources.aviation.model;

public class FlightRunways {
    private RunwayDetails scheduledDepartureRunway;
    private RunwayDetails scheduledArrivalRunway;
    private RunwayDetails actualDepartureRunway;
    private RunwayDetails actualArrivalRunway;

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

    // com.schouten.core.resources.aviation.FlightResource: FlightRunways{
    //      scheduledDepartureRunway=com.schouten.core.resources.aviation.model.RunwayDetails@44a97b43, scheduledArrivalRunway=com.schouten.core.resources.aviation.model.RunwayDetails@2d51ff04, actualDepartureRunway=com.schouten.core.resources.aviation.model.RunwayDetails@6e18ce1e, actualArrivalRunway=com.schouten.core.resources.aviation.model.RunwayDetails@48948553}

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

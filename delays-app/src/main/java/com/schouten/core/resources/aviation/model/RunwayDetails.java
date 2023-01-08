package com.schouten.core.resources.aviation.model;

public class RunwayDetails {
    private String runwayId;
    private String airportIataId;

    public RunwayDetails(String runwayId, String airportIataId) {
        this.runwayId = runwayId;
        this.airportIataId = airportIataId;
    }

    public RunwayDetails() {
    }

    public String getRunwayId() {
        return runwayId;
    }

    public String getAirportIataId() {
        return airportIataId;
    }

    @Override
    public String toString() {
        return "RunwayDetails{" +
                "runwayId='" + runwayId + '\'' +
                ", airportIataId='" + airportIataId + '\'' +
                '}';
    }
}

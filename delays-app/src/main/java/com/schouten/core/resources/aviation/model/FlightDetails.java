package com.schouten.core.resources.aviation.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class FlightDetails {
    @NotBlank private String flightNumber;
    @NotBlank private String aircraftId;
    @NotBlank private String airportId;
    @Valid @NotNull private FlightCarriers flightCarriers;
    @Valid @NotNull private FlightSchedule flightSchedule;

    public FlightDetails(String flightNumber, FlightCarriers flightCarriers, FlightSchedule flightSchedule, String aircraftId, String airportId) {
        this.flightNumber = flightNumber;
        this.flightCarriers = flightCarriers;
        this.flightSchedule = flightSchedule;
        this.aircraftId = aircraftId;
        this.airportId = airportId;
    }

    public FlightDetails() {
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public FlightCarriers getFlightCarriers() {
        return flightCarriers;
    }

    public FlightSchedule getFlightSchedule() {
        return flightSchedule;
    }

    public String getAircraftId() {
        return aircraftId;
    }

    public String getAirportId() {
        return airportId;
    }

    // Strings should be formatted like: // 2018-11-21T22:25:58+00:00
    public static Instant stringToInstant(String timeString) {
        try {
            return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timeString));
        } catch (DateTimeException dateTimeException) {
            throw new BadRequestException("Unable to parse ISO String from FlightDetails as Instant");
        }
    }
}

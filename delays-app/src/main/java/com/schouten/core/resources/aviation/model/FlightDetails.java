package com.schouten.core.resources.aviation.model;

import javax.ws.rs.BadRequestException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class FlightDetails {
    private String flightNumber;
    private FlightRunways flightRunways;
    private FlightCarriers flightCarriers;
    private FlightSchedule flightSchedule;
    private String aircraftId;

    public FlightDetails(String flightNumber, FlightRunways flightRunways, FlightCarriers flightCarriers, FlightSchedule flightSchedule, String aircraftId) {
        this.flightNumber = flightNumber;
        this.flightRunways = flightRunways;
        this.flightCarriers = flightCarriers;
        this.flightSchedule = flightSchedule;
        this.aircraftId = aircraftId;
    }

    public FlightDetails() {
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public FlightRunways getFlightRunways() {
        return flightRunways;
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

    // Strings should be formatted like: // 2018-11-21T22:25:58+00:00
    public static Instant stringToInstant(String timeString) {
        try {
            return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timeString));
        } catch (DateTimeException dateTimeException) {
            throw new BadRequestException("Unable to parse ISO String from FlightDetails as Instant");
        }
    }

    public static class Validation {
        public static String validateFlightNumber(String flightNumber) {
            if (flightNumber == null || flightNumber.isEmpty()) {
                throw new BadRequestException("Flight number is null or empty");
            }
            return  flightNumber;
        }

        public static String validateAircraftId(String aircraftId) {
            if (aircraftId == null || aircraftId.isEmpty()) {
                throw new BadRequestException("Aircraft ID is null or empty");
            }
            return aircraftId;
        }

        public static FlightSchedule validateFlightSchedule(FlightSchedule flightSchedule) {
            if (flightSchedule == null) {
                throw new BadRequestException("FlightSchedule object is null");
            }
            return flightSchedule;
        }

        public static String validateRunwayId(String runwayId) {
            if (runwayId == null || runwayId.isEmpty()) {
                throw new BadRequestException("Runway ID is null or empty");
            }
            return runwayId;
        }

        public static FlightRunways validateFlightRunways(FlightRunways flightRunways) {
            if (flightRunways == null) {
                throw new BadRequestException("FlightRunways object is null");
            }
            return flightRunways;
        }

        public static FlightCarriers validateFlightCarriers(FlightCarriers flightCarriers) {
            if (flightCarriers == null) {
                throw new BadRequestException("FlightCarriers object is null");
            }

            return flightCarriers;
        }

        public static String validateAirportId(String airportId) {
            if (airportId == null || airportId.isEmpty()) {
                throw new BadRequestException("Airport ID is null or empty");
            }
            return airportId;
        }
    }
}

package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.schouten.core.aviation.Airport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class AirportSeeder implements FlightLabsSeeder<Airport> {
    private static final String airportParam = "airports";
    private static final Logger LOGGER = LoggerFactory.getLogger(AirportSeeder.class);

    @Override
    public String getParam() {
        return airportParam;
    }

    @Override
    public Optional<Airport> constructObject(JsonNode jsonNode) {
        Optional<Airport> airport = Optional.of(new Airport(
            jsonNode.get("codeIataAirport").textValue(),
            jsonNode.get("nameAirport").textValue(),
            jsonNode.get("codeIso2Country").textValue(),
            jsonNode.get("latitudeAirport").doubleValue(),
            jsonNode.get("longitudeAirport").doubleValue()));
        LOGGER.info(airport.toString());
        return airport;
    }
}

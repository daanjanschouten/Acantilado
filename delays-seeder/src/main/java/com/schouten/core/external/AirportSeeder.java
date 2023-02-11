package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.schouten.core.ApiConstants;
import com.schouten.core.aviation.Airport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class AirportSeeder implements FlightLabsSeeder<Airport> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AirportSeeder.class);
    private static final String API_IATA_AIRPORT = "codeIataAirport";
    private static final String API_NAME_AIRPORT = "nameAirport";
    private static final String API_AIRPORT_LATITUDE = "latitudeAirport";
    private static final String API_AIRPORT_LONGITUDE = "longitudeAirport";

    @Override
    public String getApiPrefix() {
        return ApiConstants.API_AIRPORT;
    }

    @Override
    public Optional<Airport> constructObject(JsonNode jsonNode) {
        final String iataAirport = jsonNode.get(API_IATA_AIRPORT).textValue();
        final String nameAirport = jsonNode.get(API_NAME_AIRPORT).textValue();
        final String countryAirport = jsonNode.get(API_COUNTRY_ISO).textValue();
        final double latitudeAirport = jsonNode.get(API_AIRPORT_LATITUDE).doubleValue();
        final double longitudeAirport = jsonNode.get(API_AIRPORT_LONGITUDE).doubleValue();

        if (StringUtils.isEmpty(iataAirport)
                || StringUtils.isEmpty(nameAirport)
                || StringUtils.isEmpty(countryAirport)) {
            LOGGER.info("One or more required fields were empty: " + jsonNode);
            return Optional.empty();
        }

        return Optional.of(
                new Airport(iataAirport, nameAirport, countryAirport, latitudeAirport, longitudeAirport));
    }
}

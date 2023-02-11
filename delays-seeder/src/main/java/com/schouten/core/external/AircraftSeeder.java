package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.schouten.core.ApiConstants;
import com.schouten.core.aviation.Aircraft;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AircraftSeeder extends FlightLabsSeeder<Aircraft> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AircraftSeeder.class);
    private static final String PARAM_SEPARATOR = "&";
    private static final String API_HEX_ICAO_AIRPLANE = "hexIcaoAirplane";
    private static final String API_IATA_AIRLINE = "codeIataAirline";
    private static final String API_REGISTRATION_DATE = "registrationDate";
    private static final String API_PLANE_IATA = "codeIataPlaneLong";

    @Override
    protected String getApiPrefix() {
        return ApiConstants.API_AIRCRAFT;
    }

    @Override
    protected Set<String> getAdditionalParams() {
        Set<String> params = new HashSet<>();
        getCarriersToSeedFor().forEach(
                c -> params.add(StringUtils.join(PARAM_SEPARATOR, "codeIataAirline=", c)));
        return params;
    }

    @Override
    protected Optional<Aircraft> constructObject(JsonNode jsonNode) {
        final String aircraftId = jsonNode.get(API_HEX_ICAO_AIRPLANE).textValue();
        final String ownerId = jsonNode.get(API_IATA_AIRLINE).textValue();
        final String registrationDate = jsonNode.get(API_REGISTRATION_DATE).textValue();
        final String type = jsonNode.get(API_PLANE_IATA).textValue();

        if (StringUtils.isEmpty(aircraftId)
                || StringUtils.isEmpty(ownerId)
                || StringUtils.isEmpty(registrationDate)
                || StringUtils.isEmpty(type)) {
            LOGGER.info("One or more required fields were empty: " + jsonNode);
            return Optional.empty();
        }
        return Optional.of(
                new Aircraft(aircraftId, type, registrationDate, ownerId));
    }

    private static Set<String> getCarriersToSeedFor() {
        return Set.of(
                "KL",
                "AA"
        );
    }
}
package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.schouten.core.ApiConstants;
import com.schouten.core.aviation.Carrier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CarrierSeeder implements FlightLabsSeeder<Carrier> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarrierSeeder.class);
    private static final String API_CODE_HUB = "codeHub";
    private static final String API_AIRLINE_NAME = "nameAirline";

    private final static Set<String> blackList = new HashSet<>();



    @Override
    public String getApiPrefix() {
        return ApiConstants.API_CARRIER;
    }

    @Override
    public Optional<Carrier> constructObject(JsonNode jsonNode) {
        final String id = jsonNode.get(API_AIRLINE_IATA_ID).textValue();
        final String airlineName = jsonNode.get(API_AIRLINE_NAME).textValue();
        final String codeHub = jsonNode.get(API_CODE_HUB).textValue();
        final String airlineCountry = jsonNode.get(API_COUNTRY_ISO).textValue();
        if (StringUtils.isEmpty(id)
                || StringUtils.isEmpty(airlineName)
                || StringUtils.isEmpty(codeHub)
                || StringUtils.isEmpty(airlineCountry)) {
            LOGGER.info("One or more required fields were empty: " + jsonNode);
            return Optional.empty();
        }
        if (blackList.contains(id)) {
            LOGGER.info("Skipping airline because iata code already exists");
            return Optional.empty();
        }
        blackList.add(id);
        return Optional.of(
                new Carrier(id, airlineName, codeHub, airlineCountry));
    }
}

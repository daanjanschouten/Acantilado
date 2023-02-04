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

public class AircraftSeeder implements FlightLabsSeeder<Aircraft> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AircraftSeeder.class);

    @Override
    public String getParam() {
        return ApiConstants.API_AIRCRAFT;
    }

    @Override
    public Set<String> getAdditionalParams() {
        Set<String> params = new HashSet<>();
        getCarriersToSeedFor().forEach(c -> params.add(StringUtils.join("codeIataAirline=", c)));
        return params;
    }

    @Override
    public Optional<Aircraft> constructObject(JsonNode jsonNode) {
        String aircraftId = jsonNode.get("hexIcaoAirplane").textValue();
        if (StringUtils.isEmpty(aircraftId)) {
            LOGGER.info("Empty ID");
            return Optional.empty();
        }
        String ownerId = jsonNode.get("codeIataAirline").textValue();
        if (StringUtils.isEmpty(ownerId)) {
            LOGGER.info("Empty Owner");
            return Optional.empty();
        }
        String registrationDate = jsonNode.get("registrationDate").textValue();
        if (StringUtils.isEmpty(registrationDate)) {
            LOGGER.info("Empty Registration");
            return Optional.empty();
        }
        String type = jsonNode.get("codeIataPlaneLong").textValue();
        if (StringUtils.isEmpty(type)) {
            LOGGER.info("Empty Type");
            return Optional.empty();
        }
        Aircraft aircraft = new Aircraft(
                aircraftId,
                type,
                registrationDate,
                ownerId);
        LOGGER.info(aircraft.toString());
        return Optional.of(aircraft);
    }

    private static Set<String> getCarriersToSeedFor() {
        return Set.of(
                "KL",
                "AA"
        );
    }
}
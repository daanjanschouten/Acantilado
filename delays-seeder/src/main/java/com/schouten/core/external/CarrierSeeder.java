package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.schouten.core.aviation.Carrier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CarrierSeeder implements FlightLabsSeeder<Carrier> {
    private static final String carrierParam = "airlines";
    private static final Logger LOGGER = LoggerFactory.getLogger(CarrierSeeder.class);
    final private static Set<String> blackList = new HashSet<>();

    @Override
    public String getParam() {
        return carrierParam;
    }

    @Override
    public Optional<Carrier> constructObject(JsonNode jsonNode) {
        String id = jsonNode.get("codeIataAirline").textValue();
        if (StringUtils.isEmpty(id)) {
            return Optional.empty();
        }
        if (blackList.contains(id)) {
            LOGGER.info("Skipping airline because iata code already exists");
            return Optional.empty();
        }
        Optional<Carrier> carrier = Optional.of(new Carrier(
                id,
                jsonNode.get("nameAirline").textValue(),
                jsonNode.get("codeHub").textValue(),
                jsonNode.get("codeIso2Country").textValue()));
        LOGGER.info(carrier.toString());
        blackList.add(id);
        return carrier;
    }
}

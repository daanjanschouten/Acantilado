package com.acantilado.gathering.properties.collectors;

import com.acantilado.core.idealista.IdealistaLocationDAO;
import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IdealistaLocationCollector extends ApifyCollector<IdealistaAyuntamientoLocation> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaLocationCollector.class);

    private final IdealistaLocationDAO locationDAO;

    public IdealistaLocationCollector(IdealistaLocationDAO locationDAO) {
        this.locationDAO = locationDAO;
    }

    @Override
    protected IdealistaAyuntamientoLocation constructObject(JsonNode jsonNode) {
        try {
            String idealistaLocationId = jsonNode.get("locationId").textValue();
            return new IdealistaAyuntamientoLocation(idealistaLocationId);
        } catch (Exception e) {
            LOGGER.error("Failed to construct JSON object: {}", jsonNode, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void storeResult(IdealistaAyuntamientoLocation result) {
        final String locationId = result.getAyuntamientoLocationId();
        if (locationDAO.findByLocationId(locationId).isEmpty()) {
            this.locationDAO.saveOrUpdate(result);
        }
    }
}
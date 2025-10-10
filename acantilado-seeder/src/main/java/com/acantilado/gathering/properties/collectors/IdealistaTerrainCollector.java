package com.acantilado.gathering.properties.collectors;

import com.acantilado.core.idealista.IdealistaContactInformation;
import com.acantilado.core.idealista.priceRecords.IdealistaTerrainPriceRecord;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class IdealistaTerrainCollector extends ApifyCollector<IdealistaTerrain> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaTerrainCollector.class);
    private static final String SUB_TYPOLOGY_FALLBACK = "Indeterminate";

    @Override
    protected String getActorId() {
        return "REcGj6dyoIJ9Z7aE6";
    }

    @Override
    protected Optional<IdealistaTerrain> constructObject(JsonNode jsonNode) {
        try {
            Optional<IdealistaContactInformation.PhoneContact> phoneContact = constructPhone(jsonNode.get("contactInfo"));

            final long propertyCode = jsonNode.get("propertyCode").asLong();
            final long currentTimestamp = Instant.now().toEpochMilli();

            String subTypology = Objects.isNull(jsonNode.get("detailedType").get("subTypology"))
                    ? SUB_TYPOLOGY_FALLBACK
                    : jsonNode.get("detailedType").get("subTypology").textValue();
            String description = Objects.isNull(jsonNode.get("description"))
                    ? ""
                    : jsonNode.get("description").textValue();
            String contactName = Objects.isNull(jsonNode.get("contactInfo").get("contactName"))
                    ? ""
                    : jsonNode.get("contactInfo").get("contactName").textValue();

            IdealistaTerrain terrain = new IdealistaTerrain(
                    propertyCode,
                    jsonNode.get("operation").textValue(),
                    description,
                    jsonNode.get("size").longValue(),
                    subTypology,
                    jsonNode.get("address").textValue(),
                    jsonNode.get("municipality").textValue(),
                    jsonNode.get("locationId").textValue(),
                    jsonNode.get("latitude").asDouble(),
                    jsonNode.get("longitude").asDouble(),
                    currentTimestamp,
                    currentTimestamp);

            IdealistaContactInformation contactInfo = new IdealistaContactInformation(
                    phoneContact,
                    contactName,
                    jsonNode.get("contactInfo").get("userType").textValue());
            terrain.setContactInfo(contactInfo);

            IdealistaTerrainPriceRecord priceRecord = new IdealistaTerrainPriceRecord(
                    propertyCode,
                    jsonNode.get("price").longValue(),
                    currentTimestamp);
            priceRecord.setTerrain(terrain);
            terrain.getPriceRecords().add(priceRecord);

            return Optional.of(terrain);
        } catch (Exception e) {
            LOGGER.error("Failed to construct JSON object: {}", jsonNode, e);
            throw new RuntimeException(e);
        }
    }

    private static Optional<IdealistaContactInformation.PhoneContact> constructPhone(JsonNode contactJson) {
        if (! Objects.isNull(contactJson.get("phone1"))) {
            return Optional.of(new IdealistaContactInformation.PhoneContact(
                    contactJson.get("phone1").get("prefix").longValue(),
                    contactJson.get("phone1").get("phoneNumber").longValue()
            ));
        }
        return Optional.empty();
    }
}
package com.acantilado.gathering.properties.collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.acantilado.core.properties.idealista.IdealistaContactInformation;
import com.acantilado.core.properties.idealista.IdealistaPriceRecord;
import com.acantilado.core.properties.idealista.IdealistaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class IdealistaCollector extends ApifyCollector<IdealistaProperty> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollector.class);
    private static final String SUB_TYPOLOGY_FALLABACK = "Indeterminate";

    @Override
    protected String getActorId() {
        return "REcGj6dyoIJ9Z7aE6";
    }

    @Override
    protected Optional<IdealistaProperty> constructObject(JsonNode jsonNode) {
        try {
            Optional<IdealistaContactInformation.PhoneContact> phoneContact = constructPhone(jsonNode.get("contactInfo"));

            final long propertyCode = jsonNode.get("propertyCode").asLong();
            final long currentTimestamp = Instant.now().toEpochMilli();

            String subTypology = Objects.isNull(jsonNode.get("detailedType").get("subTypology"))
                    ? SUB_TYPOLOGY_FALLABACK
                    : jsonNode.get("detailedType").get("subTypology").textValue();
            String description = Objects.isNull(jsonNode.get("description"))
                    ? ""
                    : jsonNode.get("description").textValue();

            IdealistaProperty property = new IdealistaProperty(
                    propertyCode,
                    jsonNode.get("operation").textValue(),
                    description,
                    jsonNode.get("size").longValue(),
                    jsonNode.get("propertyType").textValue(),
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
                    jsonNode.get("contactInfo").get("contactName").textValue(),
                    jsonNode.get("contactInfo").get("userType").textValue());
            property.setContactInfo(contactInfo);

            IdealistaPriceRecord priceRecord = new IdealistaPriceRecord(
                    propertyCode,
                    jsonNode.get("price").longValue(),
                    currentTimestamp);
            priceRecord.setProperty(property);
            property.getPriceRecords().add(priceRecord);

            return Optional.of(property);
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



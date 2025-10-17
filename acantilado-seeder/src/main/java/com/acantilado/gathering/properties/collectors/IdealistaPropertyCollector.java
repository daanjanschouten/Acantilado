package com.acantilado.gathering.properties.collectors;

import com.acantilado.core.idealista.IdealistaContactInformation;
import com.acantilado.core.idealista.priceRecords.IdealistaPropertyPriceRecord;
import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.gathering.location.AcantiladoLocation;
import com.acantilado.gathering.location.AcantiladoLocationEstablisher;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class IdealistaPropertyCollector extends ApifyCollector<IdealistaProperty> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaPropertyCollector.class);
    private static final String SUB_TYPOLOGY_FALLBACK = "Indeterminate";

    private final AcantiladoLocationEstablisher locationEstablisher;

    public IdealistaPropertyCollector(AcantiladoLocationEstablisher locationEstablisher) {
        this.locationEstablisher = locationEstablisher;
    }

    @Override
    protected String getActorId() {
        return "REcGj6dyoIJ9Z7aE6";
    }

    @Override
    protected IdealistaProperty constructObject(JsonNode jsonNode) {
        try {
            Optional<IdealistaContactInformation.PhoneContact> phoneContact = constructPhone(jsonNode.get("contactInfo"));

            final long propertyCode = jsonNode.get("propertyCode").asLong();
            final long currentTimestamp = Instant.now().toEpochMilli();

            String propertyType = jsonNode.get("propertyType").textValue();
            String subTypology = Objects.isNull(jsonNode.get("detailedType").get("subTypology"))
                    ? SUB_TYPOLOGY_FALLBACK
                    : jsonNode.get("detailedType").get("subTypology").textValue();
            String description = Objects.isNull(jsonNode.get("description"))
                    ? ""
                    : jsonNode.get("description").textValue();
            String contactName = Objects.isNull(jsonNode.get("contactInfo").get("contactName"))
                    ? ""
                    : jsonNode.get("contactInfo").get("contactName").textValue();

            double latitude = jsonNode.get("latitude").asDouble();
            double longitude = jsonNode.get("longitude").asDouble();
            String ayuntamiento = jsonNode.get("municipality").textValue();

            AcantiladoLocation location = locationEstablisher.establish(ayuntamiento, latitude, longitude, propertyCode);

            IdealistaProperty property = new IdealistaProperty(
                    propertyCode,
                    jsonNode.get("operation").textValue(),
                    description,
                    jsonNode.get("size").longValue(),
                    propertyType,
                    subTypology,
                    jsonNode.get("address").textValue(),
                    ayuntamiento,
                    jsonNode.get("locationId").textValue(),
                    Objects.isNull(location) ? "FAILED" : location.getIdentifier(),
                    latitude,
                    longitude,
                    currentTimestamp,
                    currentTimestamp);

            // Set additional shared fields
            property.setStatus(getTextValue(jsonNode, "status"));
            property.setNewDevelopment(getBooleanValue(jsonNode, "newDevelopment"));
            property.setNewProperty(getBooleanValue(jsonNode, "newProperty"));

            // Set property-specific fields
            property.setRooms(getIntValue(jsonNode, "rooms"));
            property.setBathrooms(getIntValue(jsonNode, "bathrooms"));
            property.setFloor(getTextValue(jsonNode, "floor"));

            // Extract features
            JsonNode features = jsonNode.get("features");
            if (!Objects.isNull(features)) {
                property.setHasPool(getBooleanValue(features, "hasSwimmingPool"));
                property.setHasTerrace(getBooleanValue(features, "hasTerrace"));
                property.setHasAirConditioning(getBooleanValue(features, "hasAirConditioning"));
                property.setHasBoxRoom(getBooleanValue(features, "hasBoxRoom"));
                property.setHasGarden(getBooleanValue(features, "hasGarden"));
            }

            // Extract parking info
            JsonNode parkingSpace = jsonNode.get("parkingSpace");
            if (!Objects.isNull(parkingSpace)) {
                property.setHasParkingSpace(getBooleanValue(parkingSpace, "hasParkingSpace"));
                property.setParkingIncludedInPrice(getBooleanValue(parkingSpace, "isParkingSpaceIncludedInPrice"));
            }

            // Note: hasLift and energyCertificate might not always be in the JSON
            property.setHasLift(getBooleanValue(jsonNode, "hasLift"));
            property.setEnergyCertificate(getTextValue(jsonNode, "energyCertificate"));

            IdealistaContactInformation contactInfo = new IdealistaContactInformation(
                    phoneContact,
                    contactName,
                    jsonNode.get("contactInfo").get("userType").textValue());
            property.setContactInfo(contactInfo);

            IdealistaPropertyPriceRecord priceRecord = new IdealistaPropertyPriceRecord(
                    propertyCode,
                    jsonNode.get("price").longValue(),
                    currentTimestamp);
            priceRecord.setProperty(property);
            property.getPriceRecords().add(priceRecord);

            return property;
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

    // Helper methods to safely extract values from JSON
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return Objects.isNull(field) || field.isNull() ? null : field.textValue();
    }

    private Integer getIntValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return Objects.isNull(field) || field.isNull() ? null : field.asInt();
    }

    private Boolean getBooleanValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return Objects.isNull(field) || field.isNull() ? null : field.asBoolean();
    }
}
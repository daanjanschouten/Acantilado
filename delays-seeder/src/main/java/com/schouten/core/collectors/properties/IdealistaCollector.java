package com.schouten.core.collectors.properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.schouten.core.properties.idealista.IdealistaContactInformation;
import com.schouten.core.properties.idealista.IdealistaPriceRecord;
import com.schouten.core.properties.idealista.IdealistaProperty;

import java.time.LocalDateTime;
import java.util.Optional;

public final class IdealistaCollector extends ApifyCollector<IdealistaProperty> {

    @Override
    protected String getActorId() {
        return "REcGj6dyoIJ9Z7aE6";
    }

    @Override
    protected Optional<IdealistaProperty> constructObject(JsonNode jsonNode) {
        final long propertyCode = jsonNode.get("propertyCode").longValue();
        final LocalDateTime currentDate = LocalDateTime.now();
        final long phoneNumber = jsonNode.get("contactInfo").get("phone1").get("phoneNumber").longValue();

        IdealistaProperty property = new IdealistaProperty(
                propertyCode,
                jsonNode.get("operation").textValue(),
                jsonNode.get("description").textValue(),
                jsonNode.get("size").textValue(),
                jsonNode.get("propertyType").textValue(),
                jsonNode.get("detailedType").get("subTypology").textValue(),
                jsonNode.get("address").textValue(),
                jsonNode.get("municipality").textValue(),
                jsonNode.get("locationId").textValue(),
                jsonNode.get("latitude").textValue(),
                jsonNode.get("longitude").textValue());

        property.setContactInfo(getOrCreateContactInfo(phoneNumber, jsonNode));

        IdealistaPriceRecord priceRecord = new IdealistaPriceRecord(
                propertyCode,
                jsonNode.get("price").longValue(),
                currentDate);
        priceRecord.setProperty(property);
        property.getPriceRecords().add(priceRecord);

        return Optional.of(property);
    }

    private IdealistaContactInformation getOrCreateContactInfo(long phoneNumber, JsonNode jsonNode) {
//        IdealistaContactInformation existing = contactInfoDAO.findByPhoneNumber(phoneNumber);
//        return existing != null ? existing :
            return new IdealistaContactInformation(
                phoneNumber,
                jsonNode.get("contactInfo").get("phone1").get("prefix").longValue(),
                jsonNode.get("contactInfo").get("contactName").textValue(),
                jsonNode.get("contactInfo").get("userType").textValue());
    }
}



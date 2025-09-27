package com.schouten.core.collectors.properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schouten.core.RequestBodyData;
import com.schouten.core.collectors.properties.idealistaTypes.IdealistaCountry;
import com.schouten.core.collectors.properties.idealistaTypes.IdealistaOperation;
import com.schouten.core.collectors.properties.idealistaTypes.IdealistaPropertyType;
import com.schouten.core.collectors.properties.idealistaTypes.IdealistaSortBy;

import java.net.http.HttpRequest;

public class IdealistaData implements RequestBodyData<IdealistaData> {
    @JsonProperty("country")
    private String country;

    @JsonProperty("location")
    private String location;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("propertyType")
    private String propertyType;

    @JsonProperty("maxItems")
    private int maxItems;

    @JsonProperty("sortBy")
    private String sortBy;

    public IdealistaData(
            IdealistaCountry country,
            IdealistaOperation operation,
            IdealistaPropertyType propertyType,
            IdealistaSortBy sortBy,
            String location,
            int maxItems) {
        this.country = country.getName();
        this.operation = operation.getName();
        this.propertyType = propertyType.getName();
        this.sortBy = sortBy.getName();

        this.maxItems = maxItems;
        this.location = location;
    }

    @Override
    public HttpRequest.BodyPublisher toRequestBodyString(IdealistaData data) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(data));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }
}
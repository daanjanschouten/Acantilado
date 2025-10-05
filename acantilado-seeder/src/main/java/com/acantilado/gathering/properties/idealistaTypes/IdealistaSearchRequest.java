package com.acantilado.gathering.properties.idealistaTypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.acantilado.gathering.properties.queries.DefaultIdealistaSearchQueries;
import com.acantilado.gathering.utils.RequestBodyData;

import java.net.http.HttpRequest;

public class IdealistaSearchRequest implements RequestBodyData {
    @JsonProperty("country")
    private final String country;

    @JsonProperty("location")
    private final String location;

    @JsonProperty("operation")
    private final String operation;

    @JsonProperty("propertyType")
    private final String propertyType;

    @JsonProperty("maxItems")
    private final int maxItems;

    @JsonProperty("sortBy")
    private final String sortBy;

    public IdealistaSearchRequest(
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
    public HttpRequest.BodyPublisher toRequestBodyString() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static IdealistaSearchRequest fromSearch(DefaultIdealistaSearchQueries.IdealistaSearch search) {
        return new IdealistaSearchRequest(
                IdealistaCountry.SPAIN,
                search.operation(),
                search.type(),
                IdealistaSortBy.PROXIMITY,
                search.location(),
                1000);
    }

    @Override
    public String toString() {
        return "IdealistaSearchRequest{" +
                "country='" + country + '\'' +
                ", location='" + location + '\'' +
                ", operation='" + operation + '\'' +
                ", propertyType='" + propertyType + '\'' +
                ", maxItems=" + maxItems +
                ", sortBy='" + sortBy + '\'' +
                '}';
    }
}
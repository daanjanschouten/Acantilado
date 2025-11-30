package com.acantilado.collection.amenity;

import com.acantilado.collection.utils.RequestBodyData;
import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GoogleAmenitySearchRequest implements RequestBodyData {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAmenitySearchRequest.class);

    @JsonProperty("includeWebResults")
    private final boolean includeWebResults = false;

    @JsonProperty("language")
    private final String language = "en";

    @JsonProperty("locationQuery")
    private final String locationQuery;

    @JsonProperty("maxImages")
    private final int maxImages = 0;

    @JsonProperty("maximumLeadsEnrichmentRecords")
    private final int maximumLeadsEnrichmentRecords = 0;

    @JsonProperty("scrapeContacts")
    private final boolean scrapeContacts = false;

    @JsonProperty("scrapeDirectories")
    private final boolean scrapeDirectories = false;

    @JsonProperty("scrapeImageAuthors")
    private final boolean scrapeImageAuthors = false;

    @JsonProperty("scrapePlaceDetailPage")
    private final boolean scrapePlaceDetailPage = false;

    @JsonProperty("scrapeReviewsPersonalData")
    private final boolean scrapeReviewsPersonalData = true;

    @JsonProperty("scrapeTableReservationProvider")
    private final boolean scrapeTableReservationProvider = false;

    @JsonProperty("searchStringsArray")
    private final List<String> searchStringsArray;

    @JsonProperty("skipClosedPlaces")
    private final boolean skipClosedPlaces = false;

    @JsonProperty("searchMatching")
    private final String searchMatching = "all";

    @JsonProperty("placeMinimumStars")
    private final String placeMinimumStars = "";

    @JsonProperty("website")
    private final String website = "allPlaces";

    @JsonProperty("maxQuestions")
    private final int maxQuestions = 0;

    @JsonProperty("maxReviews")
    private final int maxReviews = 0;

    @JsonProperty("reviewsSort")
    private final String reviewsSort = "newest";

    @JsonProperty("reviewsFilterString")
    private final String reviewsFilterString = "";

    @JsonProperty("reviewsOrigin")
    private final String reviewsOrigin = "all";

    @JsonProperty("allPlacesNoSearchAction")
    private final String allPlacesNoSearchAction = "";

    public GoogleAmenitySearchRequest(String postcode, AcantiladoAmenityChain chain) {
        this.locationQuery = String.format("ES %s", postcode);
        this.searchStringsArray = List.of(chain.name().toLowerCase(Locale.ROOT));
    }

    public String getLocationQuery() {
        return locationQuery;
    }

    public List<String> getSearchStringsArray() {
        return searchStringsArray;
    }

    @Override
    public HttpRequest.BodyPublisher toRequestBodyString() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this));
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to serialize GoogleAmenitySearchRequest", exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String toString() {
        return "GoogleAmenitySearchRequest{" +
                "locationQuery='" + locationQuery + '\'' +
                ", searchStringsArray=" + searchStringsArray +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoogleAmenitySearchRequest that = (GoogleAmenitySearchRequest) o;
        return Objects.equals(locationQuery, that.locationQuery) &&
                searchStringsArray == that.searchStringsArray;
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationQuery, searchStringsArray);
    }
}
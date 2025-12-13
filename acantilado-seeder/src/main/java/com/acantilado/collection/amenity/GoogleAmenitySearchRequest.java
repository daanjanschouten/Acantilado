package com.acantilado.collection.amenity;

import com.acantilado.collection.utils.RequestBodyData;
import com.acantilado.core.amenity.fields.GoogleAmenityCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleAmenitySearchRequest implements RequestBodyData {
  private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAmenitySearchRequest.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @JsonProperty("includeWebResults")
  private final boolean includeWebResults = false;

  @JsonProperty("language")
  private final String language = "en";

  @JsonProperty("customGeolocation")
  private final JsonNode customGeolocation;

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
  private final String website;

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

  @JsonProperty("scrapeSocialMediaProfiles")
  private final Map<String, Boolean> scrapeSocialMediaProfiles =
      Map.of(
          "facebooks", false,
          "instagrams", false,
          "youtubes", false,
          "tiktoks", false,
          "twitters", false);

  public GoogleAmenitySearchRequest(String geometryGeoJson, GoogleAmenityCategory amenityType) {
    this.customGeolocation = parseGeoJson(geometryGeoJson);
    this.searchStringsArray = List.of(amenityType.getSearchTerm().toLowerCase(Locale.ROOT));
    this.website = amenityType.getExpectWebsite().getSearchTerm();
  }

  private static JsonNode parseGeoJson(String geoJson) {
    try {
      return MAPPER.readTree(geoJson);
    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to parse GeoJSON: {}", geoJson, e);
      throw new RuntimeException("Invalid GeoJSON geometry", e);
    }
  }

  public JsonNode getCustomGeolocation() {
    return customGeolocation;
  }

  public List<String> getSearchStringsArray() {
    return searchStringsArray;
  }

  public String getWebsite() {
    return website;
  }

  public Map<String, Boolean> getScrapeSocialMediaProfiles() {
    return scrapeSocialMediaProfiles;
  }

  @Override
  public HttpRequest.BodyPublisher toRequestBodyString() {
    try {
      return HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(this));
    } catch (JsonProcessingException exception) {
      LOGGER.error("Failed to serialize GoogleAmenitySearchRequest", exception);
      throw new RuntimeException(exception);
    }
  }

  @Override
  public String toString() {
    return "GoogleAmenitySearchRequest{"
        + "customGeolocation="
        + customGeolocation
        + ", searchStringsArray="
        + searchStringsArray
        + ", website="
        + website
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GoogleAmenitySearchRequest that = (GoogleAmenitySearchRequest) o;
    return Objects.equals(customGeolocation, that.customGeolocation)
        && Objects.equals(searchStringsArray, that.searchStringsArray)
        && Objects.equals(website, that.website);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customGeolocation, searchStringsArray, website);
  }
}

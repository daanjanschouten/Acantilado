package com.acantilado.collection.amenity;

import com.acantilado.collection.apify.ApifyCollector;
import com.acantilado.collection.location.AcantiladoLocation;
import com.acantilado.collection.location.AcantiladoLocationEstablisher;
import com.acantilado.core.amenity.GoogleAmenity;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.GoogleAmenitySnapshot;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.core.amenity.fields.GoogleAmenityStatus;
import com.acantilado.core.amenity.fields.OpeningHour;
import com.acantilado.core.amenity.fields.OpeningHours;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.SessionFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class GoogleAmenityCollector
    extends ApifyCollector<GoogleAmenitySearchRequest, GoogleAmenityCollector.GoogleAmenityData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAmenityCollector.class);
  private static final double LOCATION_DELTA = 0.0001; // ~10 meters
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private final GoogleAmenityDAO amenityDAO;
  private final GoogleAmenitySnapshotDAO snapshotDAO;
  private final AcantiladoLocationEstablisher locationEstablisher;

  public record GoogleAmenityData(GoogleAmenity amenity, GoogleAmenitySnapshot snapshot) {}

  public GoogleAmenityCollector(
      GoogleAmenityDAO amenityDAO,
      GoogleAmenitySnapshotDAO snapshotDAO,
      ExecutorService executorService,
      SessionFactory sessionFactory,
      AcantiladoLocationEstablisher locationEstablisher) {
    super(executorService, sessionFactory);

    this.amenityDAO = amenityDAO;
    this.snapshotDAO = snapshotDAO;
    this.locationEstablisher = locationEstablisher;
  }

  @Override
  protected String getActorId() {
    return "nwua9Gu5YrADL7ZDj";
  }

  @Override
  protected int getRetryCount() {
    return 10;
  }

  @Override
  protected int getConcurrentRunCount() {
    return 5;
  }

  @Override
  protected Optional<GoogleAmenityData> constructObject(JsonNode jsonNode) {
    try {
      String name = jsonNode.get("title").asText();
      String placeId = jsonNode.get("placeId").asText();
      String categoryName = jsonNode.get("categoryName").asText();

      Optional<AcantiladoAmenityChain> maybeChain = determineChain(name);

      JsonNode locationNode = jsonNode.get("location");
      double latitude = locationNode.get("lat").asDouble();
      double longitude = locationNode.get("lng").asDouble();

      GoogleAmenityStatus status = parseStatus(jsonNode);
      OpeningHours openingHours = parseOpeningHours(jsonNode.get("openingHours"));

      Double rating =
          jsonNode.has("totalScore") && !jsonNode.get("totalScore").isNull()
              ? jsonNode.get("totalScore").asDouble()
              : null;

      Integer userRatingCount = jsonNode.get("reviewsCount").asInt();

      Coordinate coordinate = new Coordinate(longitude, latitude);
      AcantiladoLocation location =
          locationEstablisher.establishForLocation(GEOMETRY_FACTORY.createPoint(coordinate));

      GoogleAmenity amenity =
          GoogleAmenity.builder()
              .placeId(placeId)
              .name(name)
              .latitude(latitude)
              .longitude(longitude)
              .chain(maybeChain.orElse(null))
              .category(categoryName)
              .createdAt(Instant.now())
              .acantiladoLocationId(location.getIdentifier())
              .build();

      GoogleAmenitySnapshot snapshot =
          GoogleAmenitySnapshot.builder()
              .placeId(placeId)
              .status(status)
              .openingHours(openingHours)
              .rating(rating)
              .userRatingCount(userRatingCount)
              .seenNow()
              .build();

      return Optional.of(new GoogleAmenityData(amenity, snapshot));

    } catch (Exception e) {
      LOGGER.error("Failed to construct GoogleAmenityData from JSON: {}", jsonNode, e);
      throw new RuntimeException(e);
    }
  }

  private GoogleAmenityStatus parseStatus(JsonNode jsonNode) {
    boolean permanentlyClosed =
        jsonNode.has("permanentlyClosed") && jsonNode.get("permanentlyClosed").asBoolean();
    boolean temporarilyClosed =
        jsonNode.has("temporarilyClosed") && jsonNode.get("temporarilyClosed").asBoolean();

    if (permanentlyClosed) {
      return GoogleAmenityStatus.CLOSED_PERMANENTLY;
    } else if (temporarilyClosed) {
      return GoogleAmenityStatus.CLOSED_TEMPORARILY;
    } else {
      return GoogleAmenityStatus.OPERATIONAL;
    }
  }

  private OpeningHours parseOpeningHours(JsonNode openingHoursNode) {
    OpeningHours.Builder builder = OpeningHours.builder();

    if (openingHoursNode == null || !openingHoursNode.isArray()) {
      return builder.build();
    }

    for (JsonNode dayEntry : openingHoursNode) {
      String dayName = dayEntry.get("day").asText();
      String hoursText = dayEntry.get("hours").asText();

      DayOfWeek day = parseDayName(dayName);
      parseMultipleRanges(hoursText).forEach(range -> builder.add(day, range));
    }

    return builder.build();
  }

  private DayOfWeek parseDayName(String dayName) {
    return switch (dayName) {
      case "Monday" -> DayOfWeek.MONDAY;
      case "Tuesday" -> DayOfWeek.TUESDAY;
      case "Wednesday" -> DayOfWeek.WEDNESDAY;
      case "Thursday" -> DayOfWeek.THURSDAY;
      case "Friday" -> DayOfWeek.FRIDAY;
      case "Saturday" -> DayOfWeek.SATURDAY;
      case "Sunday" -> DayOfWeek.SUNDAY;
      default -> throw new IllegalArgumentException("Unknown day name: " + dayName);
    };
  }

  // put this near other helper methods in GoogleAmenityCollector

  private List<OpeningHour> parseMultipleRanges(String text) {
    if (text == null || text.isBlank() || text.equalsIgnoreCase("Closed")) {
      return List.of();
    }

    // Normalize "Open 24 hours" (covers variants)
    if (text.equalsIgnoreCase("Open 24 hours") || text.equalsIgnoreCase("24 hours")) {
      return List.of(new OpeningHour(0, 0, 0, 0));
    }

    List<OpeningHour> result = new ArrayList<>();

    // Normalize separators: replace various dash types with " to "
    String normalized =
        text.replace("–", " to ")
            .replace("—", " to ")
            .replace("-", " to ")
            .replace("—", " to ")
            .replaceAll("\\s*to\\s*", " to "); // ensure consistent spacing

    // Split on commas for multiple ranges: "9 AM–2 PM, 5–8 PM"
    String[] ranges = normalized.split("\\s*,\\s*");

    for (String range : ranges) {
      String r = range.trim();
      if (r.isEmpty()) continue;

      // Support both "X to Y" and accidental "X–Y" forms already normalized
      String[] parts = r.split("\\s+to\\s+");
      if (parts.length != 2) {
        LOGGER.warn("Unexpected hours format (no range separator): {}", r);
        continue;
      }

      String left = parts[0].trim();
      String right = parts[1].trim();

      // detect AM/PM suffixes (case-insensitive); support e.g. "10 AM" (non-breaking spaces)
      String leftSuffix = getAmPmSuffix(left);
      String rightSuffix = getAmPmSuffix(right);

      // If only one side has suffix, propagate it
      if (leftSuffix == null && rightSuffix != null) {
        left = left + " " + rightSuffix;
        leftSuffix = rightSuffix;
      } else if (rightSuffix == null && leftSuffix != null) {
        right = right + " " + leftSuffix;
        rightSuffix = leftSuffix;
      }

      try {
        // If neither side had AM/PM, try to parse heuristically as 24-hour if a number > 12 exists
        if (leftSuffix == null && rightSuffix == null) {
          Integer lHour = peekHourNumber(left);
          Integer rHour = peekHourNumber(right);
          boolean looks24h = (lHour != null && lHour > 12) || (rHour != null && rHour > 12);

          if (looks24h) {
            // parse as 24-hour times (no AM/PM)
            TimeOfDay open = parseTimeOfDay24(left);
            TimeOfDay close = parseTimeOfDay24(right);
            result.add(new OpeningHour(open.hour, open.minute, close.hour, close.minute));
          } else {
            // Ambiguous (both <= 12, no AM/PM). Safer to skip and log.
            LOGGER.warn("Ambiguous hours (no AM/PM): {}; skipping range", r);
          }
        } else {
          // At least one side had AM/PM (we already propagated), parse both normally
          TimeOfDay open = parseTimeOfDay(left);
          TimeOfDay close = parseTimeOfDay(right);
          result.add(new OpeningHour(open.hour, open.minute, close.hour, close.minute));
        }
      } catch (Exception ex) {
        LOGGER.warn("Failed to parse hours range '{}': {}", r, ex.getMessage());
      }
    }

    return result;
  }

  /**
   * Returns "AM" or "PM" (uppercase) if the token ends with AM/PM (possibly with spaces or NBSP).
   * Otherwise null.
   */
  private String getAmPmSuffix(String token) {
    if (token == null) return null;
    String t = token.trim().toUpperCase();
    // Accept forms like "10AM", "10 AM", "10\u202FAM" etc.
    if (t.endsWith("AM")) return "AM";
    if (t.endsWith("PM")) return "PM";
    return null;
  }

  /**
   * Extract the leading numeric hour from a token (ignoring AM/PM). Returns null on parse failure.
   * e.g. "4", "4:30", "04", "04:00" -> 4
   */
  private Integer peekHourNumber(String token) {
    try {
      String t = token.trim().toUpperCase();
      // remove AM/PM if present
      if (t.endsWith("AM") || t.endsWith("PM")) {
        t = t.substring(0, t.length() - 2).trim();
      }
      // remove all whitespace and NBSPs etc
      t = t.replaceAll("[\\s\\u00A0\\u2000-\\u200B\\u202F\\u205F\\u3000]", "");
      if (t.contains(":")) {
        String[] p = t.split(":");
        return Integer.parseInt(p[0]);
      } else {
        return Integer.parseInt(t);
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse time tokens that include AM/PM. Accepts lots of whitespace and non-breaking spaces.
   * Requires AM/PM suffix (throws IllegalArgumentException if not present).
   */
  private TimeOfDay parseTimeOfDay(String timeText) {
    timeText = timeText.trim();

    boolean isPM = timeText.toUpperCase().endsWith("PM");
    boolean isAM = timeText.toUpperCase().endsWith("AM");

    if (!isPM && !isAM) {
      throw new IllegalArgumentException("Time must end with AM or PM: " + timeText);
    }

    // Remove AM/PM suffix and ALL whitespace (including Unicode spaces)
    String timeOnly =
        timeText
            .substring(0, timeText.length() - 2)
            .replaceAll("[\\s\\u00A0\\u2000-\\u200B\\u202F\\u205F\\u3000]", "");

    int hour;
    int minute = 0;

    if (timeOnly.contains(":")) {
      String[] timeParts = timeOnly.split(":");
      hour = Integer.parseInt(timeParts[0]);
      minute = Integer.parseInt(timeParts[1]);
    } else {
      hour = Integer.parseInt(timeOnly);
    }

    // Convert to 24-hour format
    if (isPM && hour != 12) {
      hour += 12;
    } else if (isAM && hour == 12) {
      hour = 0;
    }

    return new TimeOfDay(hour, minute);
  }

  /**
   * Parse time tokens WITHOUT AM/PM, assuming 24-hour format (e.g. "14:30" or "4" for 04:00).
   * Throws IllegalArgumentException on bad input.
   */
  private TimeOfDay parseTimeOfDay24(String timeText) {
    String timeOnly =
        timeText.trim().replaceAll("[\\s\\u00A0\\u2000-\\u200B\\u202F\\u205F\\u3000]", "");

    int hour;
    int minute = 0;

    if (timeOnly.contains(":")) {
      String[] timeParts = timeOnly.split(":");
      hour = Integer.parseInt(timeParts[0]);
      minute = Integer.parseInt(timeParts[1]);
    } else {
      hour = Integer.parseInt(timeOnly);
    }

    if (hour < 0 || hour > 23) {
      throw new IllegalArgumentException("Hour out of range for 24-hour time: " + hour);
    }
    if (minute < 0 || minute > 59) {
      throw new IllegalArgumentException("Minute out of range: " + minute);
    }

    return new TimeOfDay(hour, minute);
  }

  private record TimeOfDay(int hour, int minute) {}

  private Optional<AcantiladoAmenityChain> determineChain(String name) {
    String lowerName = name.toLowerCase();

    return Arrays.stream(AcantiladoAmenityChain.values())
        .filter(chain -> lowerName.contains(chain.name().toLowerCase(Locale.ROOT)))
        .findFirst();
  }

  @Override
  public void storeResult(GoogleAmenityData amenityData) {
    Optional<GoogleAmenity> existingAmenity =
        amenityDAO.findByPlaceId(amenityData.amenity().getPlaceId());
    if (existingAmenity.isEmpty()) {
      LOGGER.info("Found new amenity {}", amenityData.amenity());

      detectPlaceIdChange(amenityData.amenity());
      amenityDAO.merge(amenityData.amenity());
    }

    processSnapshot(amenityData.snapshot());
  }

  private void detectPlaceIdChange(GoogleAmenity newAmenity) {
    List<GoogleAmenity> sameLocation =
        amenityDAO.findByLocationAndChain(
            newAmenity.getLatitude(),
            newAmenity.getLongitude(),
            LOCATION_DELTA,
            newAmenity.getChain());

    if (!sameLocation.isEmpty()) {
      GoogleAmenity oldAmenity = sameLocation.get(0);
      LOGGER.warn(
          "Place ID changed from {} to {} for chain {} at location ({}, {})",
          oldAmenity.getPlaceId(),
          newAmenity.getPlaceId(),
          newAmenity.getChain(),
          newAmenity.getLatitude(),
          newAmenity.getLongitude());

      migratePlaceId(oldAmenity, newAmenity);
    }
  }

  private void migratePlaceId(GoogleAmenity oldAmenity, GoogleAmenity newAmenity) {
    newAmenity.setPreviousPlaceId(oldAmenity.getPlaceId());
    amenityDAO.merge(newAmenity);

    List<GoogleAmenitySnapshot> oldSnapshots =
        snapshotDAO.findAllByPlaceId(oldAmenity.getPlaceId());
    for (GoogleAmenitySnapshot snapshot : oldSnapshots) {
      snapshot.setPlaceId(newAmenity.getPlaceId());
      snapshotDAO.update(snapshot);
    }

    amenityDAO.delete(oldAmenity.getPlaceId());
  }

  private void processSnapshot(GoogleAmenitySnapshot newSnapshot) {
    Optional<GoogleAmenitySnapshot> latestSnapshot =
        snapshotDAO.findLatestByPlaceId(newSnapshot.getPlaceId());

    if (latestSnapshot.isEmpty()) {
      snapshotDAO.save(newSnapshot);
    } else {
      GoogleAmenitySnapshot existing = latestSnapshot.get();
      if (existing.dataMatches(newSnapshot)) {
        GoogleAmenitySnapshot updated =
            existing.withUpdatedMetadata(
                newSnapshot.getUserRatingCount().orElse(null), Instant.now());
        snapshotDAO.update(updated);
      } else {
        snapshotDAO.save(newSnapshot);
      }
    }
  }
}

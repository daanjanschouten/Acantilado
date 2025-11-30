package com.acantilado.collection.amenity;

import com.acantilado.collection.apify.ApifyCollector;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class GoogleAmenityCollector extends ApifyCollector<GoogleAmenitySearchRequest, GoogleAmenityCollector.GoogleAmenityData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAmenityCollector.class);
    private static final double LOCATION_DELTA = 0.0001; // ~10 meters

    private final GoogleAmenityDAO amenityDAO;
    private final GoogleAmenitySnapshotDAO snapshotDAO;

    public record GoogleAmenityData(GoogleAmenity amenity, GoogleAmenitySnapshot snapshot) {}

    public GoogleAmenityCollector(GoogleAmenityDAO amenityDAO, GoogleAmenitySnapshotDAO snapshotDAO, ExecutorService executorService, SessionFactory sessionFactory) {
        super(executorService, sessionFactory);

        this.amenityDAO = amenityDAO;
        this.snapshotDAO = snapshotDAO;
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

            Optional<AcantiladoAmenityChain> chain = determineChain(name);
            if (chain.isEmpty()) {
                LOGGER.warn("No chain found for name {}", name);
                return Optional.empty();
            }

            JsonNode locationNode = jsonNode.get("location");
            double latitude = locationNode.get("lat").asDouble();
            double longitude = locationNode.get("lng").asDouble();

            GoogleAmenityStatus status = parseStatus(jsonNode);
            OpeningHours openingHours = parseOpeningHours(jsonNode.get("openingHours"));

            Double rating = jsonNode.has("totalScore") && !jsonNode.get("totalScore").isNull()
                    ? jsonNode.get("totalScore").asDouble()
                    : null;

            Integer userRatingCount = jsonNode.get("reviewsCount").asInt();

            GoogleAmenity amenity = GoogleAmenity.builder()
                    .placeId(placeId)
                    .name(name)
                    .latitude(latitude)
                    .longitude(longitude)
                    .chain(chain.get())
                    .createdAt(Instant.now())
                    .build();

            GoogleAmenitySnapshot snapshot = GoogleAmenitySnapshot.builder()
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
        boolean permanentlyClosed = jsonNode.has("permanentlyClosed") && jsonNode.get("permanentlyClosed").asBoolean();
        boolean temporarilyClosed = jsonNode.has("temporarilyClosed") && jsonNode.get("temporarilyClosed").asBoolean();

        if (permanentlyClosed) {
            return GoogleAmenityStatus.CLOSED_PERMANENTLY;
        } else if (temporarilyClosed) {
            return GoogleAmenityStatus.CLOSED_TEMPORARILY;
        } else {
            return GoogleAmenityStatus.OPERATIONAL;
        }
    }

    private OpeningHours parseOpeningHours(JsonNode openingHoursNode) {
        if (openingHoursNode == null || !openingHoursNode.isArray() || openingHoursNode.isEmpty()) {
            return OpeningHours.builder().build();
        }

        OpeningHours.Builder builder = OpeningHours.builder();

        for (JsonNode dayEntry : openingHoursNode) {
            String dayName = dayEntry.get("day").asText();
            String hoursText = dayEntry.get("hours").asText();

            DayOfWeek dayOfWeek = parseDayName(dayName);
            OpeningHour hours = parseHoursText(hoursText);

            if (hours != null) {
                builder.day(dayOfWeek, hours);
            }
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

    private OpeningHour parseHoursText(String hoursText) {
        if (hoursText == null || hoursText.isEmpty() || hoursText.equalsIgnoreCase("Closed")) {
            return null;
        }

        try {
            String[] parts = hoursText.split(" to ");
            if (parts.length != 2) {
                LOGGER.warn("Unexpected hours format: {}", hoursText);
                return null;
            }

            TimeOfDay openTime = parseTimeOfDay(parts[0].trim());
            TimeOfDay closeTime = parseTimeOfDay(parts[1].trim());

            return new OpeningHour(
                    openTime.hour,
                    openTime.minute,
                    closeTime.hour,
                    closeTime.minute
            );
        } catch (Exception e) {
            LOGGER.warn("Failed to parse hours text: {}", hoursText, e);
            return null;
        }
    }

    private TimeOfDay parseTimeOfDay(String timeText) {
        timeText = timeText.trim();

        boolean isPM = timeText.endsWith("PM");
        boolean isAM = timeText.endsWith("AM");

        if (!isPM && !isAM) {
            throw new IllegalArgumentException("Time must end with AM or PM: " + timeText);
        }

        // Remove AM/PM suffix and ALL whitespace (including Unicode spaces like U+202F)
        String timeOnly = timeText.substring(0, timeText.length() - 2)
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

    private record TimeOfDay(int hour, int minute) {}

    private Optional<AcantiladoAmenityChain> determineChain(String name) {
        String lowerName = name.toLowerCase();

        return Arrays.stream(AcantiladoAmenityChain.values())
                .filter(chain -> lowerName.contains(chain.name().toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    @Override
    public void storeResult(GoogleAmenityData amenityData) {
        Optional<GoogleAmenity> existingAmenity = amenityDAO.findByPlaceId(amenityData.amenity().getPlaceId());
        if (existingAmenity.isEmpty()) {
            LOGGER.info("Found new amenity {}", amenityData.amenity());

            detectPlaceIdChange(amenityData.amenity());
            amenityDAO.merge(amenityData.amenity());
        }

        processSnapshot(amenityData.snapshot());
    }

    private void detectPlaceIdChange(GoogleAmenity newAmenity) {
        List<GoogleAmenity> sameLocation = amenityDAO.findByLocationAndChain(
                newAmenity.getLatitude(),
                newAmenity.getLongitude(),
                LOCATION_DELTA,
                newAmenity.getChain()
        );

        if (!sameLocation.isEmpty()) {
            GoogleAmenity oldAmenity = sameLocation.get(0);
            LOGGER.info("Place ID changed from {} to {} for chain {} at location ({}, {})",
                    oldAmenity.getPlaceId(), newAmenity.getPlaceId(),
                    newAmenity.getChain(), newAmenity.getLatitude(), newAmenity.getLongitude());

            migratePlaceId(oldAmenity, newAmenity);
        }
    }

    private void migratePlaceId(GoogleAmenity oldAmenity, GoogleAmenity newAmenity) {
        newAmenity.setPreviousPlaceId(oldAmenity.getPlaceId());
        amenityDAO.merge(newAmenity);

        List<GoogleAmenitySnapshot> oldSnapshots = snapshotDAO.findAllByPlaceId(oldAmenity.getPlaceId());
        for (GoogleAmenitySnapshot snapshot : oldSnapshots) {
            snapshot.setPlaceId(newAmenity.getPlaceId());
            snapshotDAO.update(snapshot);
        }

        amenityDAO.delete(oldAmenity.getPlaceId());
    }

    private void processSnapshot(GoogleAmenitySnapshot newSnapshot) {
        Optional<GoogleAmenitySnapshot> latestSnapshot = snapshotDAO.findLatestByPlaceId(newSnapshot.getPlaceId());

        if (latestSnapshot.isEmpty()) {
            snapshotDAO.save(newSnapshot);
        } else {
            GoogleAmenitySnapshot existing = latestSnapshot.get();
            if (existing.dataMatches(newSnapshot)) {
                GoogleAmenitySnapshot updated = existing.withUpdatedMetadata(
                        newSnapshot.getUserRatingCount().orElse(null),
                        Instant.now()
                );
                snapshotDAO.update(updated);
            } else {
                snapshotDAO.save(newSnapshot);
            }
        }
    }
}
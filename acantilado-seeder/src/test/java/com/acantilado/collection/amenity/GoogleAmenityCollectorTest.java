package com.acantilado.collection.amenity;

import com.acantilado.core.amenity.GoogleAmenity;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.GoogleAmenitySnapshot;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.core.amenity.fields.GoogleAmenityStatus;
import com.acantilado.core.amenity.fields.OpeningHour;
import com.acantilado.core.amenity.fields.OpeningHours;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAmenityCollectorTest {
    private GoogleAmenityCollector collector;
    private GoogleAmenityDAO amenityDAO;
    private GoogleAmenitySnapshotDAO snapshotDAO;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        amenityDAO = mock(GoogleAmenityDAO.class);
        snapshotDAO = mock(GoogleAmenitySnapshotDAO.class);
        collector = new GoogleAmenityCollector(amenityDAO, snapshotDAO, Executors.newFixedThreadPool(1), mock(SessionFactory.class));
        objectMapper = new ObjectMapper();
    }

    @Test
    void constructObject_parsesCarrefourMarket() throws IOException {
        // Load the test JSON - it's an array directly
        JsonNode amenitiesArray = loadTestJson();
        JsonNode firstPlace = amenitiesArray.get(0);

        // Execute
        GoogleAmenityCollector.GoogleAmenityData result = collector.constructObject(firstPlace);

        // Verify amenity
        GoogleAmenity amenity = result.amenity();
        assertEquals("ChIJ3dDeGiRiQQ0RG7g1xCC49EI", amenity.getPlaceId());
        assertEquals("Carrefour Market", amenity.getName());
        assertEquals(40.8110918, amenity.getLatitude(), 0.0001);
        assertEquals(-3.7688524, amenity.getLongitude(), 0.0001);
        assertEquals(AcantiladoAmenityChain.CARREFOUR, amenity.getChain());
        assertNull(amenity.getPreviousPlaceId());

        // Verify snapshot
        GoogleAmenitySnapshot snapshot = result.snapshot();
        assertEquals("ChIJ3dDeGiRiQQ0RG7g1xCC49EI", snapshot.getPlaceId());
        assertEquals(GoogleAmenityStatus.OPERATIONAL, snapshot.getStatus());
        assertEquals(3.7, snapshot.getRating().orElse(null), 0.01);
        assertEquals(480, snapshot.getUserRatingCount().orElse(null));

        // Verify opening hours
        OpeningHours hours = snapshot.getOpeningHours();
        assertTrue(hours.isOpen(DayOfWeek.MONDAY));
        assertTrue(hours.isOpen(DayOfWeek.SUNDAY));

        OpeningHour mondayHours = hours.getHours(DayOfWeek.MONDAY).orElseThrow();
        assertEquals(9, mondayHours.getOpeningHour());
        assertEquals(0, mondayHours.getOpeningMinute());
        assertEquals(21, mondayHours.getClosingHour());
        assertEquals(0, mondayHours.getClosingMinute());
    }

    @Test
    void constructObject_parsesCarrefourExpress() throws IOException {
        // Load the test JSON - it's an array directly
        JsonNode amenitiesArray = loadTestJson();
        JsonNode secondPlace = amenitiesArray.get(1);

        // Execute
        GoogleAmenityCollector.GoogleAmenityData result = collector.constructObject(secondPlace);

        // Verify
        GoogleAmenity amenity = result.amenity();
        assertEquals("ChIJrU5ELyJiQQ0RWzaw6resor8", amenity.getPlaceId());
        assertEquals("Carrefour Express EESS", amenity.getName());
        assertEquals(40.8142783, amenity.getLatitude(), 0.0001);
        assertEquals(-3.7603214, amenity.getLongitude(), 0.0001);
        assertEquals(AcantiladoAmenityChain.CARREFOUR_EXPRESS, amenity.getChain());

        GoogleAmenitySnapshot snapshot = result.snapshot();
        assertEquals(4.1, snapshot.getRating().orElse(null), 0.01);
        assertEquals(100, snapshot.getUserRatingCount().orElse(null));

        // Verify different opening hours
        OpeningHour sundayHours = snapshot.getOpeningHours().getHours(DayOfWeek.SUNDAY).orElseThrow();
        assertEquals(5, sundayHours.getOpeningHour());
        assertEquals(30, sundayHours.getOpeningMinute());
    }

    @Test
    void storeResult_newAmenity_savesAmenityAndSnapshot() throws IOException {
        // Setup
        JsonNode amenitiesArray = loadTestJson();
        JsonNode firstPlace = amenitiesArray.get(0);
        GoogleAmenityCollector.GoogleAmenityData amenityData = collector.constructObject(firstPlace);

        when(amenityDAO.findByPlaceId(any())).thenReturn(Optional.empty());
        when(amenityDAO.findByLocationAndChain(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Collections.emptyList());
        when(snapshotDAO.findLatestByPlaceId(any())).thenReturn(Optional.empty());

        // Execute
        collector.storeResult(amenityData);

        // Verify amenity was saved
        ArgumentCaptor<GoogleAmenity> amenityCaptor = ArgumentCaptor.forClass(GoogleAmenity.class);
        verify(amenityDAO).saveOrUpdate(amenityCaptor.capture());
        assertEquals("ChIJ3dDeGiRiQQ0RG7g1xCC49EI", amenityCaptor.getValue().getPlaceId());

        // Verify snapshot was saved
        ArgumentCaptor<GoogleAmenitySnapshot> snapshotCaptor = ArgumentCaptor.forClass(GoogleAmenitySnapshot.class);
        verify(snapshotDAO).save(snapshotCaptor.capture());
        assertEquals("ChIJ3dDeGiRiQQ0RG7g1xCC49EI", snapshotCaptor.getValue().getPlaceId());
    }

    @Test
    void storeResult_existingAmenityNoChange_updatesSnapshotMetadata() throws IOException {
        // Setup
        JsonNode amenitiesArray = loadTestJson();
        JsonNode firstPlace = amenitiesArray.get(0);
        GoogleAmenityCollector.GoogleAmenityData amenityData = collector.constructObject(firstPlace);

        // Mock existing amenity
        when(amenityDAO.findByPlaceId(any())).thenReturn(Optional.of(amenityData.amenity()));

        // Create old snapshot with same data but in the past
        Instant oldTime = Instant.now().minusSeconds(3600);
        GoogleAmenitySnapshot existingSnapshot = GoogleAmenitySnapshot.builder()
                .placeId(amenityData.snapshot().getPlaceId())
                .status(amenityData.snapshot().getStatus())
                .openingHours(amenityData.snapshot().getOpeningHours())
                .rating(amenityData.snapshot().getRating().orElse(null))
                .userRatingCount(amenityData.snapshot().getUserRatingCount().orElse(null))
                .firstSeen(oldTime)
                .lastSeen(oldTime)
                .build();
        when(snapshotDAO.findLatestByPlaceId(any())).thenReturn(Optional.of(existingSnapshot));

        // Execute
        collector.storeResult(amenityData);

        // Verify amenity was NOT saved (already exists)
        verify(amenityDAO, never()).saveOrUpdate(any());

        // Verify snapshot was updated (not created new)
        verify(snapshotDAO).update(any(GoogleAmenitySnapshot.class));
        verify(snapshotDAO, never()).save(any(GoogleAmenitySnapshot.class));
    }

    @Test
    void storeResult_existingAmenityWithChange_createsNewSnapshot() throws IOException {
        // Setup
        JsonNode amenitiesArray = loadTestJson();
        JsonNode firstPlace = amenitiesArray.get(0);
        GoogleAmenityCollector.GoogleAmenityData newData = collector.constructObject(firstPlace);

        // Mock existing amenity
        when(amenityDAO.findByPlaceId(any())).thenReturn(Optional.of(newData.amenity()));

        // Create old snapshot with different hours
        Instant oldTime = Instant.now().minusSeconds(3600);
        GoogleAmenitySnapshot oldSnapshot = GoogleAmenitySnapshot.builder()
                .placeId("ChIJ3dDeGiRiQQ0RG7g1xCC49EI")
                .status(GoogleAmenityStatus.OPERATIONAL)
                .openingHours(OpeningHours.builder()
                        .allDays(new OpeningHour(10, 0, 20, 0))  // Different hours
                        .build())
                .rating(3.7)
                .userRatingCount(480)
                .firstSeen(oldTime)
                .lastSeen(oldTime)
                .build();
        when(snapshotDAO.findLatestByPlaceId(any())).thenReturn(Optional.of(oldSnapshot));

        // Execute
        collector.storeResult(newData);

        // Verify new snapshot was created (not updated)
        verify(snapshotDAO).save(any(GoogleAmenitySnapshot.class));
        verify(snapshotDAO, never()).update(any(GoogleAmenitySnapshot.class));
    }

    @Test
    void storeResult_ratingCountChangeOnly_updatesMetadata() throws IOException {
        // Setup
        JsonNode amenitiesArray = loadTestJson();
        JsonNode firstPlace = amenitiesArray.get(0);
        GoogleAmenityCollector.GoogleAmenityData newData = collector.constructObject(firstPlace);

        // Mock existing amenity
        when(amenityDAO.findByPlaceId(any())).thenReturn(Optional.of(newData.amenity()));

        // Create old snapshot with different rating count but same everything else
        Instant oldTime = Instant.now().minusSeconds(3600);
        GoogleAmenitySnapshot oldSnapshot = GoogleAmenitySnapshot.builder()
                .placeId("ChIJ3dDeGiRiQQ0RG7g1xCC49EI")
                .status(GoogleAmenityStatus.OPERATIONAL)
                .openingHours(newData.snapshot().getOpeningHours())  // Same hours
                .rating(3.7)  // Same rating
                .userRatingCount(475)  // Different count
                .firstSeen(oldTime)
                .lastSeen(oldTime)
                .build();
        when(snapshotDAO.findLatestByPlaceId(any())).thenReturn(Optional.of(oldSnapshot));

        // Execute
        collector.storeResult(newData);

        // Verify snapshot was updated (not created new) because only rating count changed
        verify(snapshotDAO).update(any(GoogleAmenitySnapshot.class));
        verify(snapshotDAO, never()).save(any(GoogleAmenitySnapshot.class));
    }

    @Test
    void parseOpeningHours_allDaysSameHours() throws IOException {
        // Setup
        JsonNode amenitiesArray = loadTestJson();
        JsonNode firstPlace = amenitiesArray.get(0);

        // Execute
        GoogleAmenityCollector.GoogleAmenityData result = collector.constructObject(firstPlace);
        OpeningHours hours = result.snapshot().getOpeningHours();

        // Verify all days have same hours (9-21)
        for (DayOfWeek day : DayOfWeek.values()) {
            assertTrue(hours.isOpen(day), "Expected " + day + " to be open");
            OpeningHour dayHours = hours.getHours(day).orElseThrow();
            assertEquals(9, dayHours.getOpeningHour());
            assertEquals(21, dayHours.getClosingHour());
        }
    }

    @Test
    void parseOpeningHours_differentHoursByDay() throws IOException {
        // Setup
        JsonNode amenitiesArray = loadTestJson();
        JsonNode secondPlace = amenitiesArray.get(1);

        // Execute
        GoogleAmenityCollector.GoogleAmenityData result = collector.constructObject(secondPlace);
        OpeningHours hours = result.snapshot().getOpeningHours();

        // Verify early opening time (5:30)
        OpeningHour mondayHours = hours.getHours(DayOfWeek.MONDAY).orElseThrow();
        assertEquals(5, mondayHours.getOpeningHour());
        assertEquals(30, mondayHours.getOpeningMinute());
        assertEquals(21, mondayHours.getClosingHour());
        assertEquals(0, mondayHours.getClosingMinute());
    }

    @Test
    void determineChain_carrefourMarket() throws IOException {
        JsonNode amenitiesArray = loadTestJson();
        JsonNode firstPlace = amenitiesArray.get(0);

        GoogleAmenityCollector.GoogleAmenityData result = collector.constructObject(firstPlace);

        assertEquals(AcantiladoAmenityChain.CARREFOUR, result.amenity().getChain());
    }

    @Test
    void determineChain_carrefourExpress() throws IOException {
        JsonNode amenitiesArray = loadTestJson();
        JsonNode secondPlace = amenitiesArray.get(1);

        GoogleAmenityCollector.GoogleAmenityData result = collector.constructObject(secondPlace);

        assertEquals(AcantiladoAmenityChain.CARREFOUR_EXPRESS, result.amenity().getChain());
    }

    private JsonNode loadTestJson() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/amenities.json");
        assertNotNull(inputStream, "Test JSON file not found");
        return objectMapper.readTree(inputStream);
    }
}
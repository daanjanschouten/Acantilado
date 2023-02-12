package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schouten.core.aviation.Aircraft;
import com.schouten.core.aviation.Airport;
import com.schouten.core.aviation.Carrier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.Times;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlightLabsSeederTest {

    @Test
    void when_io_or_interrupted_exception_thrown_during_api_call_runtime_exception_is_returned() throws IOException, InterruptedException {
        AircraftSeeder mockSeeder = spy(new AircraftSeeder());

        IOException ioException = new IOException("test exception");
        doThrow(ioException).when(mockSeeder).makeApiCall(any());
        RuntimeException runtimeException = assertThrows(RuntimeException.class, mockSeeder::seed);
        assertEquals(ioException, runtimeException.getCause());

        InterruptedException interruptedException = new InterruptedException("test exception");
        doThrow(interruptedException).when(mockSeeder).makeApiCall(any());
        runtimeException = assertThrows(RuntimeException.class, mockSeeder::seed);
        assertEquals(interruptedException, runtimeException.getCause());
    }

    @Test
    void correct_uri_is_generated_during_aircraft_seeding() throws IOException, InterruptedException {
        AircraftSeeder mockSeeder = spy(new AircraftSeeder());
        ArgumentCaptor<String> prefixArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> paramArgument = ArgumentCaptor.forClass(String.class);

        // Prevent actual API calls for tests, and return sample JSON here.
        doReturn(getExampleAircraftJson()).when(mockSeeder).makeApiCall(any());

        // Start seeding process
        mockSeeder.seed();
        verify(mockSeeder, new Times(2)).constructUriString(prefixArgument.capture(), paramArgument.capture());
        assertEquals("airplanes", prefixArgument.getValue());
        List<String> capturedParameters = paramArgument.getAllValues();
        assertTrue(capturedParameters.containsAll(Set.of("&codeIataAirline=AA", "&codeIataAirline=KL")));
        assertEquals(2, capturedParameters.size());
    }

    @Test
    void correct_uri_is_generated_during_airport_seeding() throws IOException, InterruptedException {
        AirportSeeder mockSeeder = spy(new AirportSeeder());
        ArgumentCaptor<String> prefixArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> paramArgument = ArgumentCaptor.forClass(String.class);

        // Prevent actual API calls for tests, and return sample JSON here.
        doReturn(getExampleAirportJson()).when(mockSeeder).makeApiCall(any());

        // Start seeding process
        mockSeeder.seed();
        verify(mockSeeder).constructUriString(prefixArgument.capture(), paramArgument.capture());
        assertEquals("airports", prefixArgument.getValue());
        assertEquals("", paramArgument.getValue());
    }

    @Test
    void correct_uri_is_generated_during_carrier_seeding() throws IOException, InterruptedException {
        CarrierSeeder mockSeeder = spy(new CarrierSeeder());
        ArgumentCaptor<String> prefixArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> paramArgument = ArgumentCaptor.forClass(String.class);

        // Prevent actual API calls for tests, and return sample JSON here.
        doReturn(getExampleCarrierJson()).when(mockSeeder).makeApiCall(any());

        // Start seeding process
        mockSeeder.seed();
        verify(mockSeeder).constructUriString(prefixArgument.capture(), paramArgument.capture());
        assertEquals("airlines", prefixArgument.getValue());
        assertEquals("", paramArgument.getValue());
    }

    @Test
    void when_api_call_returns_six_aircraft_seeding_returns_same_six_aircraft() throws IOException, InterruptedException {
        AircraftSeeder mockSeeder = spy(new AircraftSeeder());
        // There are 3 aircraft, but we return it twice for 2 airlines, so end up with six.
        doReturn(getExampleAircraftJson()).when(mockSeeder).makeApiCall(any());

        Set<Aircraft> aircraftReturned = mockSeeder.seed();
        assertEquals(6, aircraftReturned.size());
        Set<String> expectedAircraftIcao = Set.of("A11EB7", "AC2DEA", "A9942E");
        Set<String> actualAircraftIcao = aircraftReturned
                .stream()
                .map(Aircraft::getHexIcaoId)
                .collect(Collectors.toSet());
        assertTrue(actualAircraftIcao.containsAll(expectedAircraftIcao));
    }

    @Test
    void when_api_call_returns_three_airports_seeding_returns_same_three_airports() throws IOException, InterruptedException {
        AirportSeeder mockSeeder = spy(new AirportSeeder());
        doReturn(getExampleAirportJson()).when(mockSeeder).makeApiCall(any());

        Set<Airport> airportsReturned = mockSeeder.seed();
        assertEquals(3, airportsReturned.size());
        Set<String> expectedAirportIds = Set.of("FIH", "BAX", "WED");
        Set<String> actualAirportIds = airportsReturned
                .stream()
                .map(Airport::getAirportId)
                .collect(Collectors.toSet());
        assertTrue(actualAirportIds.containsAll(expectedAirportIds));
    }

    @Test
    void when_api_call_returns_three_carriers_seeding_returns_same_three_carriers() throws IOException, InterruptedException {
        CarrierSeeder mockSeeder = spy(new CarrierSeeder());
        doReturn(getExampleCarrierJson()).when(mockSeeder).makeApiCall(any());

        Set<Carrier> carriersReturned = mockSeeder.seed();
        assertEquals(3, carriersReturned.size());
        Set<String> expectedCarrierIds = Set.of("UJ", "RF", "KT*");
        Set<String> actualCarrierIds = carriersReturned
                .stream()
                .map(Carrier::getIataId)
                .collect(Collectors.toSet());
        assertTrue(actualCarrierIds.containsAll(expectedCarrierIds));
    }

    @Test
    void when_api_call_returns_missing_fields_only_complete_aircraft_are_seeded() throws IOException, InterruptedException {
        AircraftSeeder mockSeeder = spy(new AircraftSeeder());
        // There are 3 aircraft, but we return it twice for 2 airlines, so end up with six.
        doReturn(getExampleIncompleteAircraftJson()).when(mockSeeder).makeApiCall(any());

        Set<Aircraft> aircraftReturned = mockSeeder.seed();
        assertEquals(4, aircraftReturned.size());
        Set<String> expectedAircraftIcao = Set.of("A11EB7", "AC2DEA");
        Set<String> actualAircraftIcao = aircraftReturned
                .stream()
                .map(Aircraft::getHexIcaoId)
                .collect(Collectors.toSet());
        assertTrue(actualAircraftIcao.containsAll(expectedAircraftIcao));
    }

    @Test
    void when_api_call_returns_missing_fields_only_complete_airports_are_seeded() throws IOException, InterruptedException {
        AirportSeeder mockSeeder = spy(new AirportSeeder());
        doReturn(getExampleIncompleteAirportJson()).when(mockSeeder).makeApiCall(any());

        Set<Airport> airportsReturned = mockSeeder.seed();
        assertEquals(2, airportsReturned.size());
        Set<String> expectedAirportIds = Set.of("BAX", "WED");
        Set<String> actualAirportIds = airportsReturned
                .stream()
                .map(Airport::getAirportId)
                .collect(Collectors.toSet());
        assertTrue(actualAirportIds.containsAll(expectedAirportIds));
    }

    @Test
    void when_api_call_returns_missing_fields_only_complete_carriers_are_seeded() throws IOException, InterruptedException {
        CarrierSeeder mockSeeder = spy(new CarrierSeeder());
        doReturn(getExampleIncompleteCarrierJson()).when(mockSeeder).makeApiCall(any());

        Set<Carrier> carriersReturned = mockSeeder.seed();
        assertEquals(2, carriersReturned.size());
        Set<String> expectedCarrierIds = Set.of("RF", "KT*");
        Set<String> actualCarrierIds = carriersReturned
                .stream()
                .map(Carrier::getIataId)
                .collect(Collectors.toSet());
        assertTrue(actualCarrierIds.containsAll(expectedCarrierIds));
    }

    @Test
    void when_api_call_returns_empty_json_no_aircraft_are_returned() throws IOException, InterruptedException {
        AircraftSeeder mockSeeder = spy(new AircraftSeeder());
        doReturn(getEmptyJson()).when(mockSeeder).makeApiCall(any());

        Set<Aircraft> aircraftReturned = mockSeeder.seed();
        assertEquals(0, aircraftReturned.size());
    }

    @Test
    void when_api_call_returns_empty_json_no_airports_are_returned() throws IOException, InterruptedException {
        AirportSeeder mockSeeder = spy(new AirportSeeder());
        doReturn(getEmptyJson()).when(mockSeeder).makeApiCall(any());

        Set<Airport> airportsReturned = mockSeeder.seed();
        assertEquals(0, airportsReturned.size());
    }

    @Test
    void when_api_call_returns_empty_json_no_carriers_are_returned() throws IOException, InterruptedException {
        CarrierSeeder mockSeeder = spy(new CarrierSeeder());
        doReturn(getEmptyJson()).when(mockSeeder).makeApiCall(any());

        Set<Carrier> carriersReturned = mockSeeder.seed();
        assertEquals(0, carriersReturned.size());
    }

    private JsonNode getEmptyJson() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("emptyDataJson.json");
        return new ObjectMapper().readTree(is);
    }

    private JsonNode getExampleAirportJson() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("airportJson.json");
        return new ObjectMapper().readTree(is);
    }

    private JsonNode getExampleIncompleteAirportJson() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("airportIncompleteJson.json");
        return new ObjectMapper().readTree(is);
    }

    private JsonNode getExampleCarrierJson() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("carrierJson.json");
        return new ObjectMapper().readTree(is);
    }

    private JsonNode getExampleIncompleteCarrierJson() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("carrierIncompleteJson.json");
        return new ObjectMapper().readTree(is);
    }

    private JsonNode getExampleAircraftJson() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("aircraftJson.json");
        return new ObjectMapper().readTree(is);
    }

    private JsonNode getExampleIncompleteAircraftJson() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("aircraftIncompleteJson.json");
        return new ObjectMapper().readTree(is);
    }
}
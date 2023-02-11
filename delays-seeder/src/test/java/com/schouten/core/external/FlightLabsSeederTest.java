package com.schouten.core.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FlightLabsSeederTest {

    @Test
    void correct_uri_is_generated_during_airport_seeding() throws IOException, InterruptedException {
        AirportSeeder seeder = new AirportSeeder();

        ArgumentCaptor<String> prefixArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> paramArgument = ArgumentCaptor.forClass(String.class);

        AirportSeeder mockSeeder = spy(seeder);
        // Prevent actual API calls for tests, and return sample JSON here.
        doReturn(getExampleAirportJson()).when(mockSeeder).makeApiCall(any());

        // Start seeding process
        mockSeeder.seed();
        verify(mockSeeder).constructUriString(prefixArgument.capture(), paramArgument.capture());
        assertEquals("airports", prefixArgument.getValue());
        assertEquals("", paramArgument.getValue());
    }

    private JsonNode getExampleAirportJson() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("airportJson.json");
        return new ObjectMapper().readTree(is);
    }
}
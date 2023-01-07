package com.schouten.core.resources.aviation;

import com.schouten.core.aviation.Airport;
import com.schouten.core.aviation.db.AirportDao;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(MockitoExtension.class)
public class AirportResourceTest {
    private static final AirportDao AIRPORT_DAO = mock(AirportDao.class);

    @Captor
    private static ArgumentCaptor<Airport> airportArgumentCaptor;

    @Captor
    private static ArgumentCaptor<String> iataArgumentCaptor;

    private static Airport airport;

    private static final ResourceExtension RESOURCE_EXTENSION = ResourceExtension.builder()
            .addResource(new AirportResource(AIRPORT_DAO))
            .build();

    @BeforeAll
    public static void setUp() {
        airport = new Airport(
                "AMS",
                "Schiphol",
                "The Netherlands",
                100,
                50);
    }

    @AfterEach
    public void tearDown() {
        reset(AIRPORT_DAO);
    }

    // When the DAO create method gets hit, return an airport we've already built.
    @Test
    public void createAirport() {
        when(AIRPORT_DAO.create(any(Airport.class))).thenReturn(airport);
        final Response response = RESOURCE_EXTENSION.target("/airports")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(airport, MediaType.APPLICATION_JSON_TYPE));

        Assertions.assertEquals(response.getStatusInfo(), Response.Status.OK);
        verify(AIRPORT_DAO).create(airportArgumentCaptor.capture());
        Assertions.assertEquals(airportArgumentCaptor.getValue(), airport);
    }

    @Test
    public void getAirportByIataId() {
        final String iata = "AMS";
        when(AIRPORT_DAO.findById(any(String.class))).thenReturn(Optional.ofNullable(airport));
        final Response response = RESOURCE_EXTENSION.target("/airports/getByIataId/AMS")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        Assertions.assertEquals(response.getStatusInfo(), Response.Status.OK);
        verify(AIRPORT_DAO).findById(iataArgumentCaptor.capture());
        Assertions.assertEquals(iataArgumentCaptor.getValue(), iata);
    }

    // Verify that findAll method is invoked on AirportDao
    @Test
    public void listAirports() {
        when(AIRPORT_DAO.findAll()).thenReturn(List.of(airport));
        final Response response = RESOURCE_EXTENSION.target("/airports/view")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        Assertions.assertEquals(response.getStatusInfo(), Response.Status.OK);
        verify(AIRPORT_DAO).findAll();
    }
}
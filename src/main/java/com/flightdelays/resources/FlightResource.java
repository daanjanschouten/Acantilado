package com.flightdelays.resources;

import com.flightdelays.aviation.ontology.*;
import com.flightdelays.db.FlightDao;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;

@Path("/flights")
@Produces(MediaType.APPLICATION_JSON)
public class FlightResource {
    private final FlightDao flightDao;

    public FlightResource(FlightDao flightDao) {
        this.flightDao = flightDao;
    }

    @GET
    @Path("/create")
    @UnitOfWork
    public Flight createFlight() {
        Instant sDeparture = Instant.now();
        Instant aDeparture = Instant.now();
        Instant sArrival = Instant.now();
        Instant aArrival = Instant.now();
        Aircraft aircraft = new Aircraft(12345, "modelModern");
        Airport airport = new Airport("City Airport", "The Hague", "Netherlands");
        Runway runway1 = new Runway(
                1234,
                airport
        );
        Runway runway2 = new Runway(
                1235,
                airport
        );
        Runway runway3 = new Runway(
                1236,
                airport
        );
        Carrier carrier = new Carrier(54, "myCarrier");
        Carrier operator = new Carrier(55, "myOtherCarrier");
        Flight flight = new Flight(
                "flightNumber",
                sDeparture,
                aDeparture,
                sArrival,
                aArrival,
                aircraft,
                runway1,
                runway2,
                runway3,
                carrier,
                operator);
        return flightDao.create(flight);
    }

    @GET
    @Path("/view")
    @UnitOfWork
    public List<Flight> listFlights() {
        return flightDao.findAll();
    }
}

package com.example.helloworld.resources;

import com.example.helloworld.core.Flight;
import com.example.helloworld.core.schedules.TimeSchedule;
import com.example.helloworld.db.FlightDao;
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
        TimeSchedule timeSchedule = new TimeSchedule(sDeparture, sArrival, aDeparture, aArrival);
        Flight flight = new Flight(timeSchedule);
        return flightDao.create(flight);
    }

    @GET
    @Path("/view")
    @UnitOfWork
    public List<Flight> listFlights() {
        return flightDao.findAll();
    }
}

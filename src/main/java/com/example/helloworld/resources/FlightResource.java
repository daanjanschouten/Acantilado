package com.example.helloworld.resources;

import com.example.helloworld.core.Flight;
import com.example.helloworld.db.FlightDao;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/flights")
@Produces(MediaType.APPLICATION_JSON)
public class FlightResource {
    private final FlightDao flightDao;

    public FlightResource(FlightDao flightDao) {
        this.flightDao = flightDao;
    }

    @POST
    @UnitOfWork
    public Flight createFlight(Flight flight) {
        return flightDao.create(flight);
    }

    @GET
    @UnitOfWork
    public List<Flight> listFlights() {
        return flightDao.findAll();
    }
}

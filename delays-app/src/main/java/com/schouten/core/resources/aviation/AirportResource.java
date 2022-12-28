package com.schouten.core.resources.aviation;

import com.schouten.core.aviation.Airport;
import com.schouten.core.aviation.db.AirportDao;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.BooleanParam;
import io.dropwizard.jersey.params.NonEmptyStringParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

@Path("/airports")
@Produces(MediaType.APPLICATION_JSON)
public class AirportResource {
    private final AirportDao airportDao;

    public AirportResource(AirportDao airportDao) {
        this.airportDao = airportDao;
    }

    @GET
    @Path("/getByIataId/{iataId}")
    @UnitOfWork
    public Airport getByIataId(@PathParam("iataId") NonEmptyStringParam iataId) {
        return findSafely(iataId.get().get());
    }

    @POST
    @UnitOfWork
    public Airport createAirport(Airport airport) {
        return airportDao.create(airport);
    }

    @GET
    @Path("/seed/{complete}")
    @UnitOfWork
    public long seedAirports(@PathParam("complete") BooleanParam complete) throws IOException, InterruptedException {
        return airportDao.seed(complete.get().booleanValue());
    }

    @GET
    @Path("/view")
    @UnitOfWork
    public List<Airport> listAirports() {
        return airportDao.findAll();
    }

    private Airport findSafely(String iataId) {
        return airportDao.findById(iataId).orElseThrow(() -> new NotFoundException("No such airport."));
    }
}

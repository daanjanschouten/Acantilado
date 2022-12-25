package com.aviation.resources;

import com.aviation.db.AirportDao;
import com.aviation.ontology.Airport;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.LongParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
    public Airport getByIataId(@PathParam("iataId") LongParam iataId) {
        return findSafely(iataId.get());
    }

    @POST
    @UnitOfWork
    public Airport createAirport(Airport airport) {
        return airportDao.create(airport);
    }
    @GET
    @Path("/view")
    @UnitOfWork
    public List<Airport> listAirports() {
        return airportDao.findAll();
    }

    private Airport findSafely(long iataId) {
        return airportDao.findById(iataId).orElseThrow(() -> new NotFoundException("No such airport."));
    }


}

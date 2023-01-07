package com.schouten.core.resources.aviation;


import com.schouten.core.aviation.Aircraft;
import com.schouten.core.aviation.db.AircraftDao;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.NonEmptyStringParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/aircraft")
@Produces(MediaType.APPLICATION_JSON)
public class AircraftResource {
    private final AircraftDao aircraftDao;

    public AircraftResource(AircraftDao aircraftDao) {
        this.aircraftDao = aircraftDao;
    }

    @GET
    @Path("/getByIataId/{aircraftId}")
    @UnitOfWork
    public Aircraft getByIataId(@PathParam("aircraftId") NonEmptyStringParam aircraftId) {
        return findSafely(aircraftId.get().get());
    }

    @POST
    @UnitOfWork
    public Aircraft createAircraft(Aircraft aircraft) {
        return aircraftDao.create(aircraft);
    }

    @GET
    @Path("/view")
    @UnitOfWork
    public List<Aircraft> listAircraft() {
        return aircraftDao.findAll();
    }

    private Aircraft findSafely(String aircraftId) {
        return aircraftDao.findById(aircraftId).orElseThrow(() -> new NotFoundException("No such aircraft."));
    }
}

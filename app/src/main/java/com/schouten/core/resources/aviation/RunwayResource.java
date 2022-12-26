package com.schouten.core.resources.aviation;

import com.schouten.core.aviation.Runway;
import com.schouten.core.aviation.db.RunwayDao;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.NonEmptyStringParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/runways")
@Produces(MediaType.APPLICATION_JSON)
public class RunwayResource {
    private final RunwayDao runwayDao;

    public RunwayResource(RunwayDao runwayDao) {
        this.runwayDao = runwayDao;
    }

    @GET
    @Path("/getByRunwayId/{runwayId}")
    @UnitOfWork
    public Runway getByRunwayId(@PathParam("runwayId") NonEmptyStringParam runwayId) {
        return findSafely(runwayId.get().get());
    }

    @POST
    @UnitOfWork
    public Runway createRunway(Runway runway) {
        return runwayDao.create(runway);
    }
    @GET
    @Path("/view")
    @UnitOfWork
    public List<Runway> listRunways() {
        return runwayDao.findAll();
    }

    private Runway findSafely(String runwayId) {
        return runwayDao.findById(runwayId).orElseThrow(() -> new NotFoundException("No such runway."));
    }
}

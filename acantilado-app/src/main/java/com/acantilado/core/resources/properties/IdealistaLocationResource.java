package com.acantilado.core.resources.properties;

import com.acantilado.core.idealista.IdealistaLocationDAO;
import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

@Path("/idealista-locations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IdealistaLocationResource {

    private final IdealistaLocationDAO locationDAO;

    public IdealistaLocationResource(IdealistaLocationDAO locationDAO) {
        this.locationDAO = locationDAO;
    }

    @POST
    @UnitOfWork
    public IdealistaAyuntamientoLocation createLocation(IdealistaAyuntamientoLocation location) {
        return locationDAO.create(location);
    }

    @GET
    @Path("/{locationId}")
    @UnitOfWork
    public Response getByLocationId(@PathParam("locationId") String locationId) {
        Optional<IdealistaAyuntamientoLocation> location = locationDAO.findByLocationId(locationId);
        return location
                .map(l -> Response.ok(l).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/province/{provinceId}")
    @UnitOfWork
    public List<IdealistaAyuntamientoLocation> getByProvinceId(@PathParam("provinceId") String provinceId) {
        return locationDAO.findByProvinceId(provinceId);
    }
}
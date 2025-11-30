package com.acantilado.core.resources.administrative;

import com.acantilado.core.administrative.Barrio;
import com.acantilado.core.administrative.BarrioDAO;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

@Path("/barrios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BarrioResource {

    private final BarrioDAO barrioDao;

    public BarrioResource(BarrioDAO barrioDao) {
        this.barrioDao = barrioDao;
    }

    @GET
    @Path("/list")
    @UnitOfWork
    public List<Barrio> listAll() {
        return barrioDao.findAll();
    }

    @GET
    @Path("/{id}")
    @UnitOfWork
    public Response getById(@PathParam("id") Long id) {
        Optional<Barrio> result = barrioDao.findById(id);
        if (result.isPresent()) {
            return Response.ok(result.get()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/ayuntamiento/{ayuntamientoId}")
    @UnitOfWork
    public List<Barrio> getByAyuntamiento(@PathParam("ayuntamientoId") String ayuntamientoId) {
        return barrioDao.findByAyuntamiento(ayuntamientoId);
    }

    @GET
    @Path("/search")
    @UnitOfWork
    public List<Barrio> searchByName(@QueryParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new WebApplicationException("Name query parameter is required",
                    Response.Status.BAD_REQUEST);
        }
        return barrioDao.findByName(name);
    }

    @GET
    @Path("/ayuntamiento/{ayuntamientoId}/name/{name}")
    @UnitOfWork
    public Response getByAyuntamientoAndName(
            @PathParam("ayuntamientoId") Long ayuntamientoId,
            @PathParam("name") String name) {
        Optional<Barrio> result = barrioDao.findByAyuntamientoAndName(ayuntamientoId, name);
        if (result.isPresent()) {
            return Response.ok(result.get()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @UnitOfWork
    public Barrio create(Barrio barrio) {
        return barrioDao.create(barrio);
    }
}
package com.acantilado.core.resources.administrative;

import com.acantilado.core.administrative.CodigoPostal;
import com.acantilado.core.administrative.CodigoPostalDAO;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Path("/codigos-postales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CodigoPostalResource {

    private final CodigoPostalDAO codigoPostalDao;

    public CodigoPostalResource(CodigoPostalDAO codigoPostalDao) {
        this.codigoPostalDao = codigoPostalDao;
    }

    @GET
    @Path("/list")
    @UnitOfWork
    public List<CodigoPostal> listAll() {
        return codigoPostalDao.findAll();
    }

    @GET
    @Path("/{codigoIne}")
    @UnitOfWork
    public Response getById(@PathParam("codigoIne") String codigoIne) {
        Optional<CodigoPostal> result = codigoPostalDao.findById(codigoIne);
        if (result.isPresent()) {
            return Response.ok(result.get()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/ayuntamiento/{ayuntamientoId}")
    @UnitOfWork
    public List<CodigoPostal> getByAyuntamiento(@PathParam("ayuntamientoId") String ayuntamientoId) {
        return codigoPostalDao.findByAyuntamiento(ayuntamientoId);
    }

    @GET
    @Path("/codigoPostal/{codigoPostal}")
    @UnitOfWork
    public List<CodigoPostal> getByCodigoPostal(@PathParam("codigoPostal") String codigoPostal) {
        return codigoPostalDao.findByCodigoPostal(codigoPostal);
    }

    @POST
    @UnitOfWork
    public CodigoPostal create(CodigoPostal codigoPostal) {
        return codigoPostalDao.create(codigoPostal);
    }

    @GET
    @Path("/getAllIds")
    @UnitOfWork
    public List<String> findAllIds() {
        return codigoPostalDao.findAllIds();
    }
}
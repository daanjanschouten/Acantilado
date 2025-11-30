package com.acantilado.core.resources.administrative;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/ayuntamientos")
@Produces(MediaType.APPLICATION_JSON)
public class AyuntamientoResource {
    private final AyuntamientoDAO ayuntamientoDao;

    public AyuntamientoResource(AyuntamientoDAO ayuntamientoDao) {
        this.ayuntamientoDao = ayuntamientoDao;
    }

    @POST
    @UnitOfWork
    public Ayuntamiento createAyuntamiento(Ayuntamiento ayuntamiento) {
        return ayuntamientoDao.create(ayuntamiento);
    }

    @GET
    @Path("/list")
    @UnitOfWork
    public List<Ayuntamiento> listAyuntamientos() {
        return ayuntamientoDao.findAll();
    }

    @GET
    @Path("/getById/{ayuntamientoId}")
    @UnitOfWork
    public Ayuntamiento getById(@PathParam("ayuntamientoId") String ayuntamientoId) {
        return findSafely(ayuntamientoId);
    }

    private Ayuntamiento findSafely(String ayuntamientoId) {
        return ayuntamientoDao.findById(ayuntamientoId).orElseThrow(() -> new NotFoundException("No such ayuntamiento." + ayuntamientoId));
    }

    @GET
    @Path("/getByName/{ayuntamientoName}")
    @UnitOfWork
    public List<Ayuntamiento> getByName(@PathParam("ayuntamientoName") String ayuntamientoName) {
        return ayuntamientoDao.findByName(ayuntamientoName);
    }

    @GET
    @Path("/getAllIds")
    @UnitOfWork
    public List<String> findAllIds() {
        return ayuntamientoDao.findAllIds();
    }
}


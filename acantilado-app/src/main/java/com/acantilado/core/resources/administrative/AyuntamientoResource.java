package com.acantilado.core.resources.administrative;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDao;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/ayuntamientos")
@Produces(MediaType.APPLICATION_JSON)
public class AyuntamientoResource {
    private final AyuntamientoDao ayuntamientoDao;

    public AyuntamientoResource(AyuntamientoDao ayuntamientoDao) {
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
    public Ayuntamiento getById(@PathParam("ayuntamientoId") Long ayuntamientoId) {
        return findSafely(ayuntamientoId);
    }

    private Ayuntamiento findSafely(Long ayuntamientoId) {
        return ayuntamientoDao.findById(ayuntamientoId).orElseThrow(() -> new NotFoundException("No such ayuntamiento." + ayuntamientoId));
    }
}


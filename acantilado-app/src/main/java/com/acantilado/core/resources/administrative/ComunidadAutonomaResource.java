package com.acantilado.core.resources.administrative;

import com.acantilado.core.administrative.ComunidadAutonoma;
import com.acantilado.core.administrative.ComunidadAutonomaDao;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/comunidades_autonomas")
@Produces(MediaType.APPLICATION_JSON)
public class ComunidadAutonomaResource {
    private final ComunidadAutonomaDao comunidadAutonomaDao;

    public ComunidadAutonomaResource(ComunidadAutonomaDao comunidadAutonomaDao) {
        this.comunidadAutonomaDao = comunidadAutonomaDao;
    }

    @POST
    @UnitOfWork
    public ComunidadAutonoma createComunidadAutonoma(ComunidadAutonoma comunidadAutonoma) {
        return comunidadAutonomaDao.create(comunidadAutonoma);
    }

    @GET
    @Path("/getById/{comunidadId}")
    @UnitOfWork
    public ComunidadAutonoma getById(@PathParam("comunidadId") Long comunidadId) {
        return findSafely(comunidadId);
    }

    private ComunidadAutonoma findSafely(Long comunidadId) {
        return comunidadAutonomaDao.findById(comunidadId).orElseThrow(() -> new NotFoundException("No such comunidad autonoma:" + comunidadId));
    }

    @GET
    @UnitOfWork
    public List<ComunidadAutonoma> listComunidadesAutonomas() {
        return comunidadAutonomaDao.findAll();
    }

}
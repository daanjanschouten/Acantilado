package com.schouten.core.resources.administrative;

import com.schouten.core.administrative.ComunidadAutonoma;
import com.schouten.core.administrative.db.ComunidadAutonomaDao;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/comunidades_autonomos")
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
    @UnitOfWork
    public List<ComunidadAutonoma> listComunidadesAutonomas() {
        return comunidadAutonomaDao.findAll();
    }

}
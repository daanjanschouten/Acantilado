package com.schouten.core.resources.administrative;

import com.schouten.core.administrative.Provincia;
import com.schouten.core.administrative.ProvinciaDao;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/provincias")
@Produces(MediaType.APPLICATION_JSON)
public class ProvinciaResource {
    private final ProvinciaDao provinciaDao;

    public ProvinciaResource(ProvinciaDao provinciaDao) {
        this.provinciaDao = provinciaDao;
    }

    @POST
    @UnitOfWork
    public Provincia createAyuntamiento(Provincia provincia) {
        return provinciaDao.create(provincia);
    }

    @GET
    @Path("/getById/{provinciaId}")
    @UnitOfWork
    public Provincia getById(@PathParam("provinciaId") Long provinciaId) {
        return findSafely(provinciaId);
    }

    private Provincia findSafely(Long provinciaId) {
        return provinciaDao.findById(provinciaId).orElseThrow(() -> new NotFoundException("No such provincia:" + provinciaId));
    }

    @GET
    @UnitOfWork
    public List<Provincia> listProvincias() {
        return provinciaDao.findAll();
    }

}

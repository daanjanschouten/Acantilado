package com.acantilado.core.resources.administrative;

import com.acantilado.core.administrative.Provincia;
import com.acantilado.core.administrative.ProvinciaDAO;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/provincias")
@Produces(MediaType.APPLICATION_JSON)
public class ProvinciaResource {
    private final ProvinciaDAO provinciaDao;

    public ProvinciaResource(ProvinciaDAO provinciaDao) {
        this.provinciaDao = provinciaDao;
    }

    @POST
    @UnitOfWork
    public Provincia createProvincia(Provincia provincia) {
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

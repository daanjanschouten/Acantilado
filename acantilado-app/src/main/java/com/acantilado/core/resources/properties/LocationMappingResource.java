package com.acantilado.core.resources.properties;

import com.acantilado.core.administrative.IdealistaLocationMapping;
import com.acantilado.core.administrative.IdealistaLocationMappingDAO;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/location-mappings")
@Produces(MediaType.APPLICATION_JSON)
public class LocationMappingResource {

  private final IdealistaLocationMappingDAO mappingDAO;

  public LocationMappingResource(IdealistaLocationMappingDAO mappingDAO) {
    this.mappingDAO = mappingDAO;
  }

  @POST
  @UnitOfWork
  public IdealistaLocationMapping createMapping(IdealistaLocationMapping mapping) {
    return mappingDAO.create(mapping);
  }

  @GET
  @Path("/getById/{ayuntamientoId}")
  @UnitOfWork
  public List<IdealistaLocationMapping> getById(
      @PathParam("ayuntamientoId") String ayuntamientoId) {
    return findSafely(ayuntamientoId);
  }

  private List<IdealistaLocationMapping> findSafely(String ayuntamientoId) {
    return mappingDAO.findByAyuntamientoId(ayuntamientoId);
  }

  @GET
  @UnitOfWork
  public List<IdealistaLocationMapping> listMappings() {
    return mappingDAO.findAll();
  }
}

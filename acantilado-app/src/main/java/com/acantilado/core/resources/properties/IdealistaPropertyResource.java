package com.acantilado.core.resources.properties;

import com.acantilado.core.properties.idealista.IdealistaPriceRecord;
import com.acantilado.core.properties.idealista.IdealistaPriceRecordDAO;
import com.acantilado.core.properties.idealista.IdealistaProperty;
import com.acantilado.core.properties.idealista.IdealistaPropertyDAO;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/idealista/properties")
@Produces(MediaType.APPLICATION_JSON)
public class IdealistaPropertyResource {
    private final IdealistaPropertyDAO propertyDAO;
    private final IdealistaPriceRecordDAO priceRecordDAO;

    public IdealistaPropertyResource(IdealistaPropertyDAO propertyDAO,
                                     IdealistaPriceRecordDAO priceRecordDAO) {
        this.propertyDAO = propertyDAO;
        this.priceRecordDAO = priceRecordDAO;
    }

    @POST
    @UnitOfWork
    public IdealistaProperty createProperty(IdealistaProperty property) {
        return propertyDAO.create(property);
    }

    @GET
    @Path("/list")
    @UnitOfWork
    public List<IdealistaProperty> listProperties() {
        return propertyDAO.findAll();
    }

    @GET
    @Path("/{propertyCode}")
    @UnitOfWork
    public IdealistaProperty getByPropertyCode(@PathParam("propertyCode") Long propertyCode) {
        return findSafely(propertyCode);
    }

    @GET
    @Path("/municipality/{municipality}")
    @UnitOfWork
    public List<IdealistaProperty> getByMunicipality(@PathParam("municipality") String municipality) {
        return propertyDAO.findByMunicipality(municipality);
    }

    @GET
    @Path("/ayuntamiento/{ayuntamientoId}")
    @UnitOfWork
    public List<IdealistaProperty> getByAyuntamiento(@PathParam("ayuntamientoId") Long ayuntamientoId) {
        return propertyDAO.findByAyuntamientoId(ayuntamientoId);
    }

    @GET
    @Path("/type/{propertyType}")
    @UnitOfWork
    public List<IdealistaProperty> getByPropertyType(@PathParam("propertyType") String propertyType) {
        return propertyDAO.findByPropertyType(propertyType);
    }

    @GET
    @Path("/unlinked")
    @UnitOfWork
    public List<IdealistaProperty> getUnlinkedProperties() {
        return propertyDAO.findByAyuntamientoIdIsNull();
    }

    @GET
    @Path("/{propertyCode}/price-history")
    @UnitOfWork
    public List<IdealistaPriceRecord> getPriceHistory(@PathParam("propertyCode") Long propertyCode) {
        findSafely(propertyCode);
        return priceRecordDAO.findByPropertyCode(propertyCode);
    }

    @GET
    @Path("/{propertyCode}/latest-price")
    @UnitOfWork
    public IdealistaPriceRecord getLatestPrice(@PathParam("propertyCode") Long propertyCode) {
        findSafely(propertyCode);
        return priceRecordDAO.findLatestByPropertyCode(propertyCode)
                .orElseThrow(() -> new NotFoundException("No price records found for property: " + propertyCode));
    }

    @PUT
    @Path("/{propertyCode}")
    @UnitOfWork
    public IdealistaProperty updateProperty(@PathParam("propertyCode") Long propertyCode, IdealistaProperty property) {
        findSafely(propertyCode);
        property.setPropertyCode(propertyCode);
        return propertyDAO.saveOrUpdate(property);
    }

    @DELETE
    @Path("/{propertyCode}")
    @UnitOfWork
    public Response deleteProperty(@PathParam("propertyCode") Long propertyCode) {
        IdealistaProperty property = findSafely(propertyCode);
        propertyDAO.delete(property);
        return Response.noContent().build();
    }

    private IdealistaProperty findSafely(Long propertyCode) {
        return propertyDAO.findByPropertyCode(propertyCode)
                .orElseThrow(() -> new NotFoundException("No such property: " + propertyCode));
    }
}
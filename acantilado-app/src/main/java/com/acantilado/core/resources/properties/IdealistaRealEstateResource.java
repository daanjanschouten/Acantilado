package com.acantilado.core.resources.properties;

import com.acantilado.core.idealista.IdealistaPropertyDAO;
import com.acantilado.core.idealista.IdealistaPropertyPriceRecordDAO;
import com.acantilado.core.idealista.IdealistaTerrainDAO;
import com.acantilado.core.idealista.IdealistaTerrainPriceRecordDAO;
import com.acantilado.core.idealista.priceRecords.IdealistaPropertyPriceRecord;
import com.acantilado.core.idealista.priceRecords.IdealistaTerrainPriceRecord;
import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/idealista")
@Produces(MediaType.APPLICATION_JSON)
public class IdealistaRealEstateResource {
    private final IdealistaTerrainDAO terrainDAO;
    private final IdealistaPropertyDAO propertyDAO;
    private final IdealistaPropertyPriceRecordDAO propertyPriceRecordDAO;
    private final IdealistaTerrainPriceRecordDAO terrainPriceRecordDAO;

    public IdealistaRealEstateResource(IdealistaTerrainDAO terrainDAO,
                                       IdealistaPropertyDAO propertyDAO,
                                       IdealistaPropertyPriceRecordDAO propertyPriceRecordDAO,
                                       IdealistaTerrainPriceRecordDAO terrainPriceRecordDAO) {
        this.terrainDAO = terrainDAO;
        this.propertyDAO = propertyDAO;
        this.propertyPriceRecordDAO = propertyPriceRecordDAO;
        this.terrainPriceRecordDAO = terrainPriceRecordDAO;
    }

    // Property endpoints
    @POST
    @Path("/properties")
    @UnitOfWork
    public IdealistaProperty createProperty(IdealistaProperty property) {
        return propertyDAO.create(property);
    }

    @GET
    @Path("/properties")
    @UnitOfWork
    public List<IdealistaProperty> listProperties() {
        return propertyDAO.findAll();
    }

    @GET
    @Path("/properties/{code}")
    @UnitOfWork
    public IdealistaProperty getPropertyByPropertyCode(@PathParam("code") Long code) {
        return findPropertySafely(code);
    }

    @GET
    @Path("/properties/municipality/{municipality}")
    @UnitOfWork
    public List<IdealistaProperty> getPropertiesByMunicipality(@PathParam("municipality") String municipality) {
        return propertyDAO.findByMunicipality(municipality);
    }

    @GET
    @Path("/properties/unlinked")
    @UnitOfWork
    public List<IdealistaProperty> getUnlinkedProperties() {
        return propertyDAO.findByAyuntamientoIdIsNull();
    }

    @GET
    @Path("/properties/{code}/price-history")
    @UnitOfWork
    public List<IdealistaPropertyPriceRecord> getPropertyPriceHistory(@PathParam("code") Long code) {
        findPropertySafely(code);
        return propertyPriceRecordDAO.findByPropertyCode(code);
    }

    @GET
    @Path("/properties/{code}/latest-price")
    @UnitOfWork
    public IdealistaPropertyPriceRecord getLatestPropertyPrice(@PathParam("code") Long code) {
        findPropertySafely(code);
        return propertyPriceRecordDAO.findLatestByPropertyCode(code)
                .orElseThrow(() -> new NotFoundException("No price records found for property: " + code));
    }

    @PUT
    @Path("/properties/{code}")
    @UnitOfWork
    public IdealistaProperty updateProperty(@PathParam("code") Long code, IdealistaProperty property) {
        findPropertySafely(code);
        property.setPropertyCode(code);
        return propertyDAO.merge(property);
    }

    @DELETE
    @Path("/properties/{code}")
    @UnitOfWork
    public Response deleteProperty(@PathParam("code") Long propertyCode) {
        IdealistaProperty property = findPropertySafely(propertyCode);
        propertyDAO.delete(property);
        return Response.noContent().build();
    }

    // Terrain endpoints
    @POST
    @Path("/terrains")
    @UnitOfWork
    public IdealistaTerrain createTerrain(IdealistaTerrain terrain) {
        return terrainDAO.create(terrain);
    }

    @GET
    @Path("/terrains")
    @UnitOfWork
    public List<IdealistaTerrain> listTerrains() {
        return terrainDAO.findAll();
    }

    @GET
    @Path("/terrains/{code}")
    @UnitOfWork
    public IdealistaTerrain getTerrainByPropertyCode(@PathParam("code") Long code) {
        return findTerrainSafely(code);
    }

    @GET
    @Path("/terrains/municipality/{municipality}")
    @UnitOfWork
    public List<IdealistaTerrain> getTerrainsByMunicipality(@PathParam("municipality") String municipality) {
        return terrainDAO.findByMunicipality(municipality);
    }

    @GET
    @Path("/terrains/unlinked")
    @UnitOfWork
    public List<IdealistaTerrain> getUnlinkedTerrains() {
        return terrainDAO.findByAyuntamientoIdIsNull();
    }

    @GET
    @Path("/terrains/{code}/price-history")
    @UnitOfWork
    public List<IdealistaTerrainPriceRecord> getTerrainPriceHistory(@PathParam("code") Long code) {
        findTerrainSafely(code);
        return terrainPriceRecordDAO.findByPropertyCode(code);
    }

    @GET
    @Path("/terrains/{code}/latest-price")
    @UnitOfWork
    public IdealistaTerrainPriceRecord getLatestTerrainPrice(@PathParam("code") Long code) {
        findTerrainSafely(code);
        return terrainPriceRecordDAO.findLatestByPropertyCode(code)
                .orElseThrow(() -> new NotFoundException("No price records found for terrain: " + code));
    }

    @PUT
    @Path("/terrains/{code}")
    @UnitOfWork
    public IdealistaTerrain updateTerrain(@PathParam("code") Long code, IdealistaTerrain terrain) {
        findTerrainSafely(code);
        terrain.setPropertyCode(code);
        return terrainDAO.merge(terrain);
    }

    @DELETE
    @Path("/terrains/{code}")
    @UnitOfWork
    public Response deleteTerrain(@PathParam("code") Long propertyCode) {
        IdealistaTerrain terrain = findTerrainSafely(propertyCode);
        terrainDAO.delete(terrain);
        return Response.noContent().build();
    }

    // Helper methods
    private IdealistaProperty findPropertySafely(Long propertyCode) {
        return propertyDAO.findByPropertyCode(propertyCode)
                .orElseThrow(() -> new NotFoundException("No such property: " + propertyCode));
    }

    private IdealistaTerrain findTerrainSafely(Long propertyCode) {
        return terrainDAO.findByPropertyCode(propertyCode)
                .orElseThrow(() -> new NotFoundException("No such terrain: " + propertyCode));
    }
}
package com.schouten.core.resources.aviation;

import com.schouten.core.aviation.Carrier;
import com.schouten.core.aviation.db.CarrierDao;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.BooleanParam;
import io.dropwizard.jersey.params.NonEmptyStringParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

@Path("/carriers")
@Produces(MediaType.APPLICATION_JSON)
public class CarrierResource {
    private final CarrierDao carrierDao;

    public CarrierResource(CarrierDao CarrierDao) {
        this.carrierDao = CarrierDao;
    }

    @GET
    @Path("/getByIataId/{carrierId}")
    @UnitOfWork
    public Carrier getByIataId(@PathParam("carrierId") NonEmptyStringParam carrierId) {
        return findSafely(carrierId.get().get());
    }

    @GET
    @Path("/seed/{complete}")
    @UnitOfWork
    public long seedCarriers(@PathParam("complete") BooleanParam complete) throws IOException, InterruptedException {
        return carrierDao.seed(complete.get().booleanValue());
    }

    @POST
    @UnitOfWork
    public Carrier createCarrier(Carrier carrier) {
        return carrierDao.create(carrier);
    }

    @GET
    @Path("/view")
    @UnitOfWork
    public List<Carrier> listCarriers() {
        return carrierDao.findAll();
    }

    private Carrier findSafely(String carrierId) {
        return carrierDao.findById(carrierId).orElseThrow(() -> new NotFoundException("No such carrier."));
    }
}

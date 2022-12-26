package com.schouten.core.resources.aviation;

import com.schouten.core.aviation.Carrier;
import com.schouten.core.aviation.db.CarrierDao;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.NonEmptyStringParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/carriers")
@Produces(MediaType.APPLICATION_JSON)
public class CarrierResource {
    private final CarrierDao CarrierDao;

    public CarrierResource(CarrierDao CarrierDao) {
        this.CarrierDao = CarrierDao;
    }

    @GET
    @Path("/getByIataId/{carrierId}")
    @UnitOfWork
    public Carrier getByIataId(@PathParam("carrierId") NonEmptyStringParam carrierId) {
        return findSafely(carrierId.get().get());
    }

    @POST
    @UnitOfWork
    public Carrier createCarrier(Carrier carrier) {
        return CarrierDao.create(carrier);
    }
    @GET
    @Path("/view")
    @UnitOfWork
    public List<Carrier> listCarriers() {
        return CarrierDao.findAll();
    }

    private Carrier findSafely(String carrierId) {
        return CarrierDao.findById(carrierId).orElseThrow(() -> new NotFoundException("No such carrier."));
    }
}

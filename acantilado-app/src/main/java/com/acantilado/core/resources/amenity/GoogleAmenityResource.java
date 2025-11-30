package com.acantilado.core.resources.amenity;

import com.acantilado.core.amenity.GoogleAmenity;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.core.amenity.fields.AcantiladoAmenityType;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/amenities")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Amenities", description = "Endpoints for Google amenities")
public class GoogleAmenityResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAmenityResource.class);
    private final GoogleAmenityDAO amenityDAO;

    public GoogleAmenityResource(GoogleAmenityDAO amenityDAO) {
        this.amenityDAO = amenityDAO;
    }

    @GET
    @UnitOfWork
    @Operation(summary = "List all amenities")
    public List<GoogleAmenity> listAll() {
        return amenityDAO.findAll();
    }

    @GET
    @Path("/{placeId}")
    @UnitOfWork
    @Operation(summary = "Get an amenity by its place ID")
    public Response getByPlaceId(@PathParam("placeId") String placeId) {
        Optional<GoogleAmenity> amenity = amenityDAO.findByPlaceId(placeId);

        if (amenity.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Amenity not found"))
                    .build();
        }

        return Response.ok(amenity.get()).build();
    }

    @GET
    @Path("/chain/{chain}")
    @UnitOfWork
    @Operation(summary = "Get all amenities for a given chain")
    public Response getByChain(@PathParam("chain") String chainName) {
        try {
            AcantiladoAmenityChain chain = AcantiladoAmenityChain.valueOf(chainName.toUpperCase());
            List<GoogleAmenity> amenities = amenityDAO.findByChain(chain);
            return Response.ok(amenities).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid chain name: " + chainName))
                    .build();
        }
    }

    @GET
    @Path("/type/{type}")
    @UnitOfWork
    @Operation(summary = "Get all amenities for a given amenity type")
    public Response getByType(@PathParam("type") String typeName) {
        try {
            AcantiladoAmenityType type = AcantiladoAmenityType.valueOf(typeName.toUpperCase());
            List<GoogleAmenity> amenities = amenityDAO.findByType(type);
            return Response.ok(amenities).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid type name: " + typeName))
                    .build();
        }
    }

    /**
     * GET /amenities/location?lat={lat}&lng={lng}&delta={delta}&chain={chain}
     * Find amenities near a location
     * Example: /amenities/location?lat=40.4168&lng=-3.7038&delta=0.01&chain=CARREFOUR
     */
    @GET
    @Path("/location")
    @UnitOfWork
    public Response getByLocation(
            @QueryParam("lat") Double latitude,
            @QueryParam("lng") Double longitude,
            @QueryParam("delta") @DefaultValue("0.01") Double delta,
            @QueryParam("chain") String chainName) {

        if (latitude == null || longitude == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "lat and lng parameters are required"))
                    .build();
        }

        List<GoogleAmenity> amenities;

        if (chainName != null && !chainName.isEmpty()) {
            try {
                AcantiladoAmenityChain chain = AcantiladoAmenityChain.valueOf(chainName.toUpperCase());
                amenities = amenityDAO.findByLocationAndChain(latitude, longitude, delta, chain);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid chain name: " + chainName))
                        .build();
            }
        } else {
            amenities = amenityDAO.findByLocation(latitude, longitude, delta);
        }

        return Response.ok(amenities).build();
    }

    /**
     * GET /amenities/previous/{previousPlaceId}
     * Find amenity by its previous Place ID (for tracking migrations)
     */
    @GET
    @Path("/previous/{previousPlaceId}")
    @UnitOfWork
    public Response getByPreviousPlaceId(@PathParam("previousPlaceId") String previousPlaceId) {
        Optional<GoogleAmenity> amenity = amenityDAO.findByPreviousPlaceId(previousPlaceId);

        if (amenity.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "No amenity found with previous Place ID: " + previousPlaceId))
                    .build();
        }

        return Response.ok(amenity.get()).build();
    }

    /**
     * GET /amenities/stats
     * Get statistics about amenities
     */
    @GET
    @Path("/stats")
    @UnitOfWork
    @Operation(summary = "Get stats across all amenities")
    public Response getStats() {
        long totalCount = amenityDAO.countAll();

        Map<String, Long> chainCounts = Map.of(
                "CARREFOUR", amenityDAO.countByChain(AcantiladoAmenityChain.CARREFOUR),
                "MERCADONA", amenityDAO.countByChain(AcantiladoAmenityChain.MERCADONA),
                "DIA", amenityDAO.countByChain(AcantiladoAmenityChain.DIA),
                "LIDL", amenityDAO.countByChain(AcantiladoAmenityChain.LIDL),
                "ALDI", amenityDAO.countByChain(AcantiladoAmenityChain.ALDI)
        );

        return Response.ok(Map.of(
                "totalAmenities", totalCount,
                "countsByChain", chainCounts
        )).build();
    }
}
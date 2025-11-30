package com.acantilado.core.resources.amenity;

import com.acantilado.core.amenity.GoogleAmenitySnapshot;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.core.amenity.fields.GoogleAmenityStatus;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/snapshots")
@Produces(MediaType.APPLICATION_JSON)
public class GoogleAmenitySnapshotResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAmenitySnapshotResource.class);
    private final GoogleAmenitySnapshotDAO snapshotDAO;

    public GoogleAmenitySnapshotResource(GoogleAmenitySnapshotDAO snapshotDAO) {
        this.snapshotDAO = snapshotDAO;
    }

    /**
     * GET /snapshots
     * List all snapshots
     */
    @GET
    @UnitOfWork
    public List<GoogleAmenitySnapshot> listAll() {
        return snapshotDAO.findAll();
    }

    /**
     * GET /snapshots/latest/{placeId}
     * Get the most recent snapshot for a place
     */
    @GET
    @Path("/latest/{placeId}")
    @UnitOfWork
    public Response getLatest(@PathParam("placeId") String placeId) {
        Optional<GoogleAmenitySnapshot> snapshot = snapshotDAO.findLatestByPlaceId(placeId);

        if (snapshot.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "No snapshots found for Place ID: " + placeId))
                    .build();
        }

        return Response.ok(snapshot.get()).build();
    }

    /**
     * GET /snapshots/{placeId}
     * Get all snapshots for a place (ordered by most recent)
     */
    @GET
    @Path("/{placeId}")
    @UnitOfWork
    public Response getAllForPlace(@PathParam("placeId") String placeId) {
        List<GoogleAmenitySnapshot> snapshots = snapshotDAO.findAllByPlaceId(placeId);

        if (snapshots.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "No snapshots found for Place ID: " + placeId))
                    .build();
        }

        return Response.ok(snapshots).build();
    }

    /**
     * GET /snapshots/{placeId}/range?start={iso}&end={iso}
     * Get snapshots within a time range
     * Example: /snapshots/ChIJ123/range?start=2025-01-01T00:00:00Z&end=2025-12-31T23:59:59Z
     */
    @GET
    @Path("/{placeId}/range")
    @UnitOfWork
    public Response getByTimeRange(
            @PathParam("placeId") String placeId,
            @QueryParam("start") String startTime,
            @QueryParam("end") String endTime) {

        if (startTime == null || endTime == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "start and end parameters are required (ISO 8601 format)"))
                    .build();
        }

        try {
            Instant start = Instant.parse(startTime);
            Instant end = Instant.parse(endTime);

            List<GoogleAmenitySnapshot> snapshots = snapshotDAO.findByPlaceIdAndTimeRange(placeId, start, end);
            return Response.ok(snapshots).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid datetime format. Use ISO 8601: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /snapshots/chain/{chain}/range?start={iso}&end={iso}
     * Get snapshots for a chain within a time range
     * Example: /snapshots/chain/CARREFOUR/range?start=2025-01-01T00:00:00Z&end=2025-12-31T23:59:59Z
     */
    @GET
    @Path("/chain/{chain}/range")
    @UnitOfWork
    public Response getByChainAndTimeRange(
            @PathParam("chain") String chainName,
            @QueryParam("start") String startTime,
            @QueryParam("end") String endTime) {

        if (startTime == null || endTime == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "start and end parameters are required (ISO 8601 format)"))
                    .build();
        }

        try {
            AcantiladoAmenityChain chain = AcantiladoAmenityChain.valueOf(chainName.toUpperCase());
            Instant start = Instant.parse(startTime);
            Instant end = Instant.parse(endTime);

            List<GoogleAmenitySnapshot> snapshots = snapshotDAO.findByChainAndTimeRange(chain, start, end);
            return Response.ok(snapshots).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid chain name: " + chainName))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid datetime format: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /snapshots/status/{status}?days={days}
     * Find snapshots where status changed to a specific status
     * Example: /snapshots/status/CLOSED_PERMANENTLY?days=30
     */
    @GET
    @Path("/status/{status}")
    @UnitOfWork
    public Response getByStatusChange(
            @PathParam("status") String statusName,
            @QueryParam("days") @DefaultValue("30") int daysAgo) {

        try {
            GoogleAmenityStatus status = GoogleAmenityStatus.valueOf(statusName.toUpperCase());
            Instant since = Instant.now().minus(daysAgo, ChronoUnit.DAYS);

            List<GoogleAmenitySnapshot> snapshots = snapshotDAO.findByStatusChange(status, since);
            return Response.ok(snapshots).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid status name: " + statusName))
                    .build();
        }
    }

    /**
     * GET /snapshots/stale?days={days}
     * Find amenities that haven't been seen in X days
     * Example: /snapshots/stale?days=7
     */
    @GET
    @Path("/stale")
    @UnitOfWork
    public Response getStaleSnapshots(@QueryParam("days") @DefaultValue("7") int days) {
        Instant notSeenSince = Instant.now().minus(days, ChronoUnit.DAYS);
        List<GoogleAmenitySnapshot> snapshots = snapshotDAO.findStaleSnapshots(notSeenSince);
        return Response.ok(snapshots).build();
    }

    /**
     * GET /snapshots/rating-changes?minChange={change}&days={days}
     * Find significant rating changes
     * Example: /snapshots/rating-changes?minChange=1.0&days=30
     */
    @GET
    @Path("/rating-changes")
    @UnitOfWork
    public Response getRatingChanges(
            @QueryParam("minChange") @DefaultValue("1.0") double minChange,
            @QueryParam("days") @DefaultValue("30") int days) {

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<GoogleAmenitySnapshot> snapshots = snapshotDAO.findSignificantRatingChanges(minChange, since);
        return Response.ok(snapshots).build();
    }

    /**
     * GET /snapshots/stats
     * Get statistics about snapshots
     */
    @GET
    @Path("/stats")
    @UnitOfWork
    public Response getStats() {
        long totalCount = snapshotDAO.countAll();

        return Response.ok(Map.of(
                "totalSnapshots", totalCount
        )).build();
    }

    /**
     * GET /snapshots/{placeId}/count
     * Count snapshots for a specific place
     */
    @GET
    @Path("/{placeId}/count")
    @UnitOfWork
    public Response getCount(@PathParam("placeId") String placeId) {
        long count = snapshotDAO.countByPlaceId(placeId);
        return Response.ok(Map.of("placeId", placeId, "snapshotCount", count)).build();
    }
}
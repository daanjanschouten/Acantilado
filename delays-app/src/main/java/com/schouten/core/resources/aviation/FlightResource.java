package com.schouten.core.resources.aviation;

import com.schouten.core.aviation.*;
import com.schouten.core.aviation.db.*;
import com.schouten.core.resources.aviation.model.FlightCarriers;
import com.schouten.core.resources.aviation.model.FlightDetails;
import com.schouten.core.resources.aviation.model.FlightDetails.Validation;
import com.schouten.core.resources.aviation.model.FlightRunways;
import com.schouten.core.resources.aviation.model.FlightSchedule;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Path("/flights")
@Produces(MediaType.APPLICATION_JSON)
public class FlightResource {
    private final FlightDao flightDao;
    private final RunwayDao runwayDao;
    private final CarrierDao carrierDao;
    private final AircraftDao aircraftDao;
    private final AirportDao airportDao;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlightResource.class);

    public FlightResource(FlightDao flightDao, RunwayDao runwayDao, CarrierDao carrierDao, AircraftDao aircraftDao, AirportDao airportDao) {
        this.flightDao = flightDao;
        this.runwayDao = runwayDao;
        this.carrierDao = carrierDao;
        this.aircraftDao = aircraftDao;
        this.airportDao = airportDao;
    }

    @POST
    @Path("/create")
    @UnitOfWork
    public Flight createFlight(@Valid FlightDetails flightDetails) {
        if (flightDetails == null) {
            throw new BadRequestException("No FlightDetails object was provided");
        }

        String flightNumber = Validation.validateFlightNumber(flightDetails.getFlightNumber());
        Optional<Aircraft> aircraft = aircraftDao.findById(
                Validation.validateAircraftId(flightDetails.getAircraftId()));
        if (aircraft.isEmpty()) {
            aircraft = Optional.of(new Aircraft(
                    "aircraft123",
                    "boeing14"
            ));
        }

        FlightSchedule flightSchedule = Validation.validateFlightSchedule(flightDetails.getFlightSchedule());
        Instant sDeparture = FlightDetails.stringToInstant(flightSchedule.getScheduledDeparture());
        Instant sArrival = FlightDetails.stringToInstant(flightSchedule.getScheduledArrival());
        Instant aDeparture = FlightDetails.stringToInstant(flightSchedule.getActualDeparture());
        Instant aArrival = FlightDetails.stringToInstant(flightSchedule.getActualArrival());

        FlightRunways flightRunways = Validation.validateFlightRunways(flightDetails.getFlightRunways());
        LOGGER.info(flightRunways.toString());
        Runway sDepartureRunway = retrieveOrCreateRunway(
                flightRunways.getScheduledDepartureRunway().getRunwayId(),
                flightRunways.getScheduledDepartureRunway().getAirportIataId());
        Runway sArrivalRunway = retrieveOrCreateRunway(
                flightRunways.getScheduledArrivalRunway().getRunwayId(),
                flightRunways.getScheduledArrivalRunway().getAirportIataId());
        Runway aDepartureRunway = retrieveOrCreateRunway(
                flightRunways.getActualDepartureRunway().getRunwayId(),
                flightRunways.getActualDepartureRunway().getAirportIataId());
        Runway aArrivalRunway = retrieveOrCreateRunway(
                flightRunways.getActualArrivalRunway().getRunwayId(),
                flightRunways.getActualArrivalRunway().getAirportIataId());

        FlightCarriers flightCarriers = Validation.validateFlightCarriers(flightDetails.getFlightCarriers());
        Optional<Carrier> carrier = carrierDao.findById(flightCarriers.getCarrierId());
        Optional<Carrier> operator = carrierDao.findById(flightCarriers.getOperatorId());
        Carrier testCarrier = new Carrier("54", "myCarrier");

        Flight flight = new Flight(
                flightNumber,
                sDeparture,
                sArrival,
                aDeparture,
                aArrival,
                aircraft.get(),
                sDepartureRunway,
                sArrivalRunway,
                aDepartureRunway,
                aArrivalRunway,
                carrier.orElse(testCarrier),
                operator.orElse(testCarrier));

        return flightDao.create(flight);
    }

    private Runway retrieveOrCreateRunway(String runwayId, String airportId) {
        Optional<Runway> storedRunway = runwayDao.findById(runwayId);
        if (storedRunway.isPresent()) {
            return storedRunway.get();
        }
        Optional<Airport> airport = airportDao.findById(airportId);
        if (airport.isEmpty()) {
            LOGGER.warn(StringUtils.join("Airport with ID not found in database: ", airportId));
            throw new BadRequestException("Airport with that ID not found during Runway creation");
        }
        return runwayDao.create(
                new Runway(runwayId, airport.get()));
    }

    @GET
    @Path("/view")
    @UnitOfWork
    public List<Flight> listFlights() {
        return flightDao.findAll();
    }
}

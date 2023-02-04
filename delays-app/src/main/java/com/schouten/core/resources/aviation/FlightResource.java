 package com.schouten.core.resources.aviation;

 import com.schouten.core.aviation.Aircraft;
 import com.schouten.core.aviation.Airport;
 import com.schouten.core.aviation.Carrier;
 import com.schouten.core.aviation.Flight;
 import com.schouten.core.aviation.db.*;
 import com.schouten.core.resources.aviation.model.FlightCarriers;
 import com.schouten.core.resources.aviation.model.FlightDetails;
 import com.schouten.core.resources.aviation.model.FlightSchedule;
 import io.dropwizard.hibernate.UnitOfWork;
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
    private final CarrierDao carrierDao;
    private final AircraftDao aircraftDao;
    private final AirportDao airportDao;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlightResource.class);

    public FlightResource(FlightDao flightDao, CarrierDao carrierDao, AircraftDao aircraftDao, AirportDao airportDao) {
        this.flightDao = flightDao;
        this.carrierDao = carrierDao;
        this.aircraftDao = aircraftDao;
        this.airportDao = airportDao;
    }

    @POST
    @Path("/create")
    @UnitOfWork
    public Flight createFlight(@Valid FlightDetails flightDetails) {
        String flightNumber = flightDetails.getFlightNumber();


        FlightSchedule flightSchedule = flightDetails.getFlightSchedule();
        Instant sDeparture = FlightDetails.stringToInstant(flightSchedule.getScheduledDeparture());
        Instant sArrival = FlightDetails.stringToInstant(flightSchedule.getScheduledArrival());
        Instant aDeparture = FlightDetails.stringToInstant(flightSchedule.getActualDeparture());
        Instant aArrival = FlightDetails.stringToInstant(flightSchedule.getActualArrival());

        Optional<Aircraft> aircraft = aircraftDao.findById(flightDetails.getAircraftId()) ;
        if (aircraft.isEmpty()) {
            aircraft = Optional.of(new Aircraft(
                    "aircraft123",
                    "boeing14"
            ));
        }

        Optional<Airport> airport = airportDao.findById(flightDetails.getAirportId());
        if (airport.isEmpty()) {
            throw new BadRequestException("Airport not found");
        }

        FlightCarriers flightCarriers = flightDetails.getFlightCarriers();
        Optional<Carrier> carrier = carrierDao.findById(flightCarriers.getCarrierId());
        Optional<Carrier> operator = carrierDao.findById(flightCarriers.getOperatorId());
        if (carrier.isEmpty() || operator.isEmpty()) {
            throw new BadRequestException("Carrier not found");
        }

        Flight flight = new Flight(
                flightNumber,
                sDeparture,
                sArrival,
                aDeparture,
                aArrival,
                airport.get(),
                aircraft.get(),
                carrier.get(),
                operator.get());

        return flightDao.create(flight);
    }

//    private Runway retrieveOrCreateRunway(String runwayId, String airportId) {
//        Optional<Runway> storedRunway = runwayDao.findById(runwayId);
//        if (storedRunway.isPresent()) {
//            return storedRunway.get();
//        }
//        Optional<Airport> airport = airportDao.findById(airportId);
//        if (airport.isEmpty()) {
//            LOGGER.warn(StringUtils.join("Airport with ID not found in database: ", airportId));
//            throw new BadRequestException("Airport with that ID not found during Runway creation");
//        }
//        return runwayDao.create(
//                new Runway(runwayId, airport.get()));
//    }

    @GET
    @Path("/view")
    @UnitOfWork
    public List<Flight> listFlights() {
        return flightDao.findAll();
    }
}

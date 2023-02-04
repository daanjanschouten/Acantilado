package com.schouten.core.aviation.db;

import com.schouten.core.aviation.Airport;
import com.schouten.core.external.AirportSeeder;
import io.dropwizard.hibernate.AbstractDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AirportDao extends AbstractDAO<Airport> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AirportDao.class);
    public AirportDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Airport> findById(String iataId) {
        return Optional.ofNullable(get(iataId));
    }

    public Airport create(Airport airport) {
        return persist(airport);
    }

    public long seed(boolean complete) throws IOException, InterruptedException {
        Set<Airport> allAirports = new AirportSeeder().seed();
        if (!complete) {
            List<String> existingAirports = findAll().stream()
                    .map(Airport::getAirportId)
                    .collect(Collectors.toList());
            List<Airport> newAirports = allAirports.stream()
                    .filter(a -> ! existingAirports.contains(a.getAirportId()))
                    .collect(Collectors.toList());
            newAirports.forEach(this::create);
            LOGGER.info(StringUtils.join(
                    "Retrieved ", allAirports.size(), " airports, of which ", existingAirports.size(),
                    " already exist. ", newAirports.size(), " were added: ", newAirports.toString()));
            return newAirports.size();
        }
        allAirports.forEach(this::create);
        LOGGER.info(StringUtils.join("Completely reseeded and added ", allAirports.size(), " new airports."));
        return allAirports.size();
    }

    public List<Airport> findAll() {
        return list(
                namedTypedQuery("com.flightdelays.core.Airport.findAll")
        );
    }
}

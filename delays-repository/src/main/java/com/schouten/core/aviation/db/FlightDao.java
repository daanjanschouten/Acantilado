package com.schouten.core.aviation.db;

import com.schouten.core.aviation.Flight;
import io.dropwizard.hibernate.AbstractDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class FlightDao extends AbstractDAO<Flight> {
    public FlightDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(FlightDao.class);

    public Optional<Flight> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public Flight create(Flight flight) {
        LOGGER.info(StringUtils.join("Creating a new flight: ", flight));
        return persist(flight);
    }

    public List<Flight> findAll() {
        return list(
                namedTypedQuery("com.example.helloworld.core.Flight.findAll")
        );
    }
}
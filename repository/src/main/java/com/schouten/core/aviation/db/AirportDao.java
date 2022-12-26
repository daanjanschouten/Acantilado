package com.schouten.core.aviation.db;

import com.schouten.core.aviation.Airport;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class AirportDao extends AbstractDAO<Airport> {
    public AirportDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Airport> findById(Long iataId) {
        return Optional.ofNullable(get(iataId));
    }

    public Airport create(Airport airport) {
        return persist(airport);
    }

    public List<Airport> findAll() {
        return list(
                namedTypedQuery("com.flightdelays.core.Airport.findAll")
        );
    }
}

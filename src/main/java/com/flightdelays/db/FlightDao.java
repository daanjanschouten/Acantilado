package com.flightdelays.db;

import com.flightdelays.aviation.ontology.Flight;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class FlightDao extends AbstractDAO<Flight> {
    public FlightDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Flight> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public Flight create(Flight flight) {
        return persist(flight);
    }

    public List<Flight> findAll() {
        return list(
                namedTypedQuery("com.example.helloworld.core.Flight.findAll")
        );
    }
}


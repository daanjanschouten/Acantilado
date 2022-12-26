package com.schouten.core.aviation.db;

import com.schouten.core.aviation.Aircraft;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class AircraftDao extends AbstractDAO<Aircraft> {
    public AircraftDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Aircraft> findById(String aircraftId) {
        return Optional.ofNullable(get(aircraftId));
    }

    public Aircraft create(Aircraft aircraft) {
        return persist(aircraft);
    }

    public List<Aircraft> findAll() {
        return list(
                namedTypedQuery("com.flightdelays.core.Aircraft.findAll")
        );
    }
}

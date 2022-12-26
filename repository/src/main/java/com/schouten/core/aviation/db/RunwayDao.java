package com.schouten.core.aviation.db;

import com.schouten.core.aviation.Runway;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class RunwayDao extends AbstractDAO<Runway> {
    public RunwayDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Runway> findById(String runwayId) {
        return Optional.ofNullable(get(runwayId));
    }

    public Runway create(Runway runway) {
        return persist(runway);
    }

    public List<Runway> findAll() {
        return list(
                namedTypedQuery("com.flightdelays.core.Runway.findAll")
        );
    }
}
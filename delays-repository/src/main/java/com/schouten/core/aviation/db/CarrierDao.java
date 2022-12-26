package com.schouten.core.aviation.db;

import com.schouten.core.aviation.Carrier;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class CarrierDao extends AbstractDAO<Carrier> {
    public CarrierDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Carrier> findById(String CarrierId) {
        return Optional.ofNullable(get(CarrierId));
    }

    public Carrier create(Carrier Carrier) {
        return persist(Carrier);
    }

    public List<Carrier> findAll() {
        return list(
                namedTypedQuery("com.flightdelays.core.Carrier.findAll")
        );
    }
}

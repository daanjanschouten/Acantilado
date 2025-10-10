package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class CodigoPostalDAO extends AbstractDAO<CodigoPostal> {

    public CodigoPostalDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public CodigoPostal create(CodigoPostal codigoPostal) {
        return persist(codigoPostal);
    }

    public Optional<CodigoPostal> findById(String codigoPostal) {
        return Optional.ofNullable(get(codigoPostal));
    }

    public List<CodigoPostal> findAll() {
        return list(namedTypedQuery("com.acantilado.codigopostal.findAll"));
    }

    public List<CodigoPostal> findByAyuntamiento(Long ayuntamientoId) {
        return list(
                namedTypedQuery("com.acantilado.codigopostal.findByAyuntamiento")
                        .setParameter("ayuntamiento_id", ayuntamientoId)
        );
    }

    public void delete(CodigoPostal codigoPostal) {
        currentSession().delete(codigoPostal);
    }
}

package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class BarrioDAO extends AbstractDAO<Barrio> {

    public BarrioDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Barrio create(Barrio barrio) {
        return persist(barrio);
    }

    public Optional<Barrio> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public List<Barrio> findAll() {
        return list(namedTypedQuery("com.acantilado.barrio.findAll"));
    }

    public List<Barrio> findByAyuntamiento(Long ayuntamientoId) {
        return list(
                namedTypedQuery("com.acantilado.barrio.findByAyuntamiento")
                        .setParameter("ayuntamiento_id", ayuntamientoId)
        );
    }

    public List<Barrio> findByName(String name) {
        return list(
                namedTypedQuery("com.acantilado.barrio.findByName")
                        .setParameter("name", name)
        );
    }

    public Optional<Barrio> findByAyuntamientoAndName(Long ayuntamientoId, String name) {
        List<Barrio> results = list(
                namedTypedQuery("com.acantilado.barrio.findByAyuntamientoAndName")
                        .setParameter("ayuntamiento_id", ayuntamientoId)
                        .setParameter("name", name)
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void delete(Barrio barrio) {
        currentSession().delete(barrio);
    }
}
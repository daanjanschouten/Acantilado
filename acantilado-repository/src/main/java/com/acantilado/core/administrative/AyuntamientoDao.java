package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class AyuntamientoDAO extends AbstractDAO<Ayuntamiento> {
    public AyuntamientoDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Ayuntamiento> findById(Long ayuntamientoId) {
        return Optional.ofNullable(get(ayuntamientoId));
    }

    public Ayuntamiento create(Ayuntamiento ayuntamiento) {
        return merge(ayuntamiento);
    }

    private Ayuntamiento merge(Ayuntamiento ayuntamiento) {
        return (Ayuntamiento) currentSession().merge(ayuntamiento);
    }

    public List<Ayuntamiento> findAll() {
        return list(
                namedTypedQuery("com.acantilado.ayuntamiento.findAll")
        );
    }

    public List<Ayuntamiento> findByProvinceId(long provinceId) {
        return namedTypedQuery("com.acantilado.ayuntamiento.findByProvinceId")
                .setParameter("provincia_id", provinceId)
                .getResultList();
    }

    public List<Ayuntamiento> findByName(String name) {
        return namedTypedQuery("com.acantilado.ayuntamiento.findByName")
                .setParameter("name", name)
                .getResultList();
    }

    public List<Long> findAllIds() {
        return currentSession()
                .createQuery("SELECT a.ayuntamiento_id FROM Ayuntamiento a", Long.class)
                .getResultList();
    }
}
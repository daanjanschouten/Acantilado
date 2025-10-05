package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class AyuntamientoDao extends AbstractDAO<Ayuntamiento> {
    public AyuntamientoDao(SessionFactory sessionFactory) {
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
}
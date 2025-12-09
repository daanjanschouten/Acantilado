package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class ProvinciaDAO extends AbstractDAO<Provincia> {
    // private static final Logger LOGGER = LoggerFactory.getLogger(ProvinciaDao.class);

    public ProvinciaDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Provincia> findById(String provinciaId) {
        return Optional.ofNullable(get(provinciaId));
    }

    public Provincia create(Provincia provincia) {
        return persist(provincia);
    }

    public List<Provincia> findAll() {
        return list(
                namedTypedQuery("com.schouten.core.provincia.findAll")
        );
    }

    public List<Provincia> findByName(String name) {
        return namedTypedQuery("com.acantilado.provincia.findByName")
                .setParameter("name", name)
                .getResultList();
    }

    public List<Long> findAllIds() {
        return currentSession()
                .createQuery("SELECT p.provincia_id FROM Provincia p", Long.class)
                .getResultList();
    }
}
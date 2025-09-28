package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class ProvinciaDao extends AbstractDAO<Provincia> {
    // private static final Logger LOGGER = LoggerFactory.getLogger(ProvinciaDao.class);

    public ProvinciaDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Provincia> findById(Long provinciaId) {
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
}
package com.schouten.core.administrative.db;

import com.schouten.core.administrative.Ayuntamiento;
import com.schouten.core.seeding.administration.AyuntamientoCollector;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class AyuntamientoDao extends AbstractDAO<Ayuntamiento> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AyuntamientoDao.class);

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
                namedTypedQuery("com.schouten.core.ayuntamiento.findAll")
        );
    }

    public void seed() {
        Iterator<Collection<Ayuntamiento>> iterator = new AyuntamientoCollector().seed();

        while(iterator.hasNext()) {
            Collection<Ayuntamiento> collection = iterator.next();
            collection.forEach(this::create);
        }
    }
}
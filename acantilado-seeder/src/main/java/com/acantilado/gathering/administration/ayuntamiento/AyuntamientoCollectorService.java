package com.acantilado.gathering.administration.ayuntamiento;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.gathering.administration.CollectorService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

public final class AyuntamientoCollectorService extends CollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AyuntamientoCollectorService.class);

    private final AyuntamientoDAO ayuntamientoDao;
    private final SessionFactory sessionFactory;

    public AyuntamientoCollectorService(AyuntamientoDAO ayuntamientoDao, SessionFactory sessionFactory) {
        this.ayuntamientoDao = ayuntamientoDao;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public boolean isSeedingNecessary() {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);

        try {
            return ayuntamientoDao.findAll().isEmpty();
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    @Override
    public void seed() {
        LOGGER.info("Starting to seed ayuntamiento table");
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        ManagedSessionContext.bind(session);

        try {
            Iterator<Collection<Ayuntamiento>> iterator = new AyuntamientoCollector().seed();
            while (iterator.hasNext()) {
                iterator.next().forEach(ayuntamientoDao::create);
            }
            transaction.commit();
            LOGGER.info("Finished seeding ayuntamiento table");
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException("Seeding failed for ayuntamientos", e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }
}

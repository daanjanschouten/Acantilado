package com.acantilado.gathering.administration;

import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;

import java.util.Collection;
import java.util.Iterator;

public final class AyuntamientoCollectorService {
    private final AyuntamientoDao ayuntamientoDao;
    private final SessionFactory sessionFactory;

    public AyuntamientoCollectorService(AyuntamientoDao ayuntamientoDao, SessionFactory sessionFactory) {
        this.ayuntamientoDao = ayuntamientoDao;
        this.sessionFactory = sessionFactory;
    }

    public void seed() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        ManagedSessionContext.bind(session);

        try {
            // Check if seeding is necessary
            if (ayuntamientoDao.findById(1001L).isEmpty()) {
                Iterator<Collection<Ayuntamiento>> iterator = new AyuntamientoCollector().seed();
                while (iterator.hasNext()) {
                    iterator.next().forEach(ayuntamientoDao::create);
                }
                transaction.commit();
            }
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException("Seeding failed for ayuntamientos", e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }
}

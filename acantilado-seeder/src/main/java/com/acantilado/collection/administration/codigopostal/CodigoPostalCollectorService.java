package com.acantilado.collection.administration.codigopostal;

import com.acantilado.collection.administration.CollectorService;
import com.acantilado.core.administrative.CodigoPostal;
import com.acantilado.core.administrative.CodigoPostalDAO;
import java.util.Iterator;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodigoPostalCollectorService extends CollectorService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CodigoPostalCollectorService.class);

  private final CodigoPostalDAO codigoPostalDAO;
  private final SessionFactory sessionFactory;

  public CodigoPostalCollectorService(
      CodigoPostalDAO codigoPostalDAO, SessionFactory sessionFactory) {
    this.codigoPostalDAO = codigoPostalDAO;
    this.sessionFactory = sessionFactory;
  }

  @Override
  public boolean isSeedingNecessary() {
    Session session = sessionFactory.openSession();
    ManagedSessionContext.bind(session);

    try {
      return codigoPostalDAO.findAll().isEmpty();
    } finally {
      ManagedSessionContext.unbind(sessionFactory);
      session.close();
    }
  }

  @Override
  public void seed() {
    LOGGER.info("Starting to seed codigo postal table");

    Session session = sessionFactory.openSession();
    Transaction transaction = session.beginTransaction();
    ManagedSessionContext.bind(session);

    try {
      Iterator<Set<CodigoPostal>> iterator = new CodigoPostalCollector().seed();
      int count = 0;

      while (iterator.hasNext()) {
        Set<CodigoPostal> batch = iterator.next();
        for (CodigoPostal codigoPostal : batch) {
          codigoPostalDAO.create(codigoPostal);
          count++;

          // Flush periodically to avoid memory issues
          if (count % 100 == 0) {
            session.flush();
            session.clear();
            LOGGER.debug("Seeded {} postal codes...", count);
          }
        }
      }

      session.flush();
      transaction.commit();

      LOGGER.info("Successfully seeded {} postal codes", count);

    } catch (Exception e) {
      transaction.rollback();
      throw new RuntimeException("Postal code seeding failed", e);
    } finally {
      ManagedSessionContext.unbind(sessionFactory);
      session.close();
    }
  }
}

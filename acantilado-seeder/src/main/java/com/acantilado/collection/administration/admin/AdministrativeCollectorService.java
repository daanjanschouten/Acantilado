package com.acantilado.collection.administration.admin;

import com.acantilado.collection.administration.CollectorService;
import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.Provincia;
import com.acantilado.core.administrative.ProvinciaDAO;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdministrativeCollectorService extends CollectorService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AdministrativeCollectorService.class);

  private final AyuntamientoDAO ayuntamientoDao;
  private final ProvinciaDAO provinciaDAO;
  private final SessionFactory sessionFactory;

  public AdministrativeCollectorService(
      ProvinciaDAO provinciaDAO, AyuntamientoDAO ayuntamientoDao, SessionFactory sessionFactory) {
    this.ayuntamientoDao = ayuntamientoDao;
    this.provinciaDAO = provinciaDAO;
    this.sessionFactory = sessionFactory;
  }

  @Override
  public boolean isSeedingNecessary() {
    Session session = sessionFactory.openSession();
    ManagedSessionContext.bind(session);

    try {
      return ayuntamientoDao.findAllIds().size() != 8132;
    } finally {
      ManagedSessionContext.unbind(sessionFactory);
      session.close();
    }
  }

  @Override
  public void seed() {
    LOGGER.info("Starting to seed administrative tables");
    Session session = sessionFactory.openSession();
    Transaction transaction = session.beginTransaction();
    ManagedSessionContext.bind(session);

    try {
      Set<Provincia> provinces = new ProvinciaCollector().seed();
      provinces.forEach(provinciaDAO::create);

      Set<Ayuntamiento> ayuntamientos = new AyuntamientoCollector().seed();
      ayuntamientos.forEach(ayuntamientoDao::create);

      transaction.commit();
      LOGGER.info("Finished seeding administrative tables");
    } catch (Exception e) {
      transaction.rollback();
      throw new RuntimeException("Seeding failed for administrative tables", e);
    } finally {
      ManagedSessionContext.unbind(sessionFactory);
      session.close();
    }

    //        AyuntamientoExporter ayuntamientoExporter = new AyuntamientoExporter(sessionFactory);
    //
    // ayuntamientoExporter.exportProvince29("./acantilado-seeder/src/main/resources/output.geojson");
  }
}

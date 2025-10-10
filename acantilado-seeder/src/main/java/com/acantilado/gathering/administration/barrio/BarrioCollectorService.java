package com.acantilado.gathering.administration.barrio;

import com.acantilado.core.administrative.BarrioDAO;
import com.acantilado.gathering.administration.CollectorService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BarrioCollectorService extends CollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BarrioCollectorService.class);

    private final BarrioDAO barrioDAO;
    private final SessionFactory sessionFactory;

    private static final List<CityBarrioConfig> CITY_CONFIGS = List.of(
            new CityBarrioConfig(28079L, "/barrios/madrid_barrios.geojson", "NOMBRE"),
            new CityBarrioConfig(8019L, "/barrios/barcelona_barrios.geojson", "NOM"),
            new CityBarrioConfig(46250L, "/barrios/valencia_barrios.geojson", "nombre"),
            new CityBarrioConfig(50297L, "/barrios/zaragoza_barrios.geojson", "nombre"),
            new CityBarrioConfig(41091L, "/barrios/sevilla_barrios.geojson", "BARRIO"),
            new CityBarrioConfig(29067L, "/barrios/malaga_barrios.geojson", "NOMBARRIO"),
            new CityBarrioConfig(20069L, "/barrios/sansebastian_barrios.geojson", "NOM_EUS"),
            new CityBarrioConfig(3014L, "/barrios/alicante_barrios.geojson", "barrio"),
            new CityBarrioConfig(38038L, "/barrios/tenerife_barrios.geojson", "BARRIO"),
            new CityBarrioConfig(30024L, "/barrios/lorca_barrios.geojson", "NNCLEC")
    );

    public record CityBarrioConfig(Long ayuntamientoId, String geoJsonPath, String barrioTitle) {}

    public BarrioCollectorService(BarrioDAO barrioDAO, SessionFactory sessionFactory) {
        this.barrioDAO = barrioDAO;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public boolean isSeedingNecessary() {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            return barrioDAO.findAll().isEmpty();
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    @Override
    public void seed() {
//        if (!isSeedingNecessary()) {
//            LOGGER.info("Barrios already seeded, skipping...");
//            return;
//        }

        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            ManagedSessionContext.bind(session);
            LOGGER.info("Starting barrio seeding for {} cities...", CITY_CONFIGS.size());

            AtomicInteger totalBarriosSeeded = new AtomicInteger(0);
            for (CityBarrioConfig config : CITY_CONFIGS) {
                BarrioCollector collector = new BarrioCollector(config);

                collector.collect().forEach(barrio -> {
                    barrioDAO.create(barrio);
                    totalBarriosSeeded.incrementAndGet();

                    if (totalBarriosSeeded.get() % 50 == 0) {
                        session.flush();
                        session.clear();
                        LOGGER.debug("Seeded {} barrios...", totalBarriosSeeded);
                    }
                });
            }

            session.flush();
            transaction.commit();
            LOGGER.info("Successfully seeded {} total barrios across {} cities", totalBarriosSeeded, CITY_CONFIGS.size());
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException("Barrio seeding failed", e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }
}
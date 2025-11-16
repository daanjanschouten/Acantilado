package com.acantilado.collection.administration.barrio;

import com.acantilado.core.administrative.BarrioDAO;
import com.acantilado.collection.administration.CollectorService;
import com.acantilado.collection.location.CityAyuntamientoCode;
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
            new CityBarrioConfig(CityAyuntamientoCode.MADRID, "/barrios/madrid_barrios.geojson", "NOMBRE"),
            new CityBarrioConfig(CityAyuntamientoCode.BARCELONA, "/barrios/barcelona_barrios.geojson", "NOM"),
            new CityBarrioConfig(CityAyuntamientoCode.VALENCIA, "/barrios/valencia_barrios.geojson", "nombre"),
            new CityBarrioConfig(CityAyuntamientoCode.ZARAGOZA, "/barrios/zaragoza_barrios.geojson", "nombre"),
            new CityBarrioConfig(CityAyuntamientoCode.SEVILLA, "/barrios/sevilla_barrios.geojson", "BARRIO"),
            new CityBarrioConfig(CityAyuntamientoCode.MALAGA, "/barrios/malaga_barrios.geojson", "NOMBARRIO"),
            new CityBarrioConfig(CityAyuntamientoCode.SAN_SEBASTIAN, "/barrios/sansebastian_barrios.geojson", "NOM_EUS"),
            new CityBarrioConfig(CityAyuntamientoCode.ALICANTE, "/barrios/alicante_barrios.geojson", "barrio"),
            // new CityBarrioConfig(CityAyuntamientoCode.TENERIFE, "/barrios/tenerife_barrios.geojson", "BARRIO"),
            new CityBarrioConfig(CityAyuntamientoCode.LORCA, "/barrios/lorca_barrios.geojson", "NNCLEC")
    );

    public record CityBarrioConfig(CityAyuntamientoCode ayuntamientoCode, String geoJsonPath, String barrioTitle) {}

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
        if (!isSeedingNecessary()) {
            LOGGER.info("Barrios already seeded, skipping...");
            return;
        }

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
package com.acantilado.gathering.administration;

import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.BarrioDAO;
import com.acantilado.core.administrative.CodigoPostalDAO;
import com.acantilado.gathering.administration.ayuntamiento.AyuntamientoCollectorService;
import com.acantilado.gathering.administration.barrio.BarrioCollectorService;
import com.acantilado.gathering.administration.codigopostal.CodigoPostalCollectorService;
import com.google.common.base.Stopwatch;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class AdministrativeCollectorService extends CollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdministrativeCollectorService.class);

    private final CodigoPostalCollectorService codigoPostalCollectorService;
    private final AyuntamientoCollectorService ayuntamientoCollectorService;
    private final BarrioCollectorService barrioCollectorService;
    private final CodigoPostalToAyuntamientoLinkingService codigoPostalToAyuntamientoLinkingService;

    public AdministrativeCollectorService(CodigoPostalDAO codigoPostalDAO, AyuntamientoDAO ayuntamientoDAO, BarrioDAO barrioDAO, SessionFactory sessionFactory) {
        this.codigoPostalCollectorService = new CodigoPostalCollectorService(codigoPostalDAO, sessionFactory);
        this.ayuntamientoCollectorService = new AyuntamientoCollectorService(ayuntamientoDAO, sessionFactory);
        this.barrioCollectorService = new BarrioCollectorService(barrioDAO, sessionFactory);

        this.codigoPostalToAyuntamientoLinkingService =
                new CodigoPostalToAyuntamientoLinkingService(ayuntamientoDAO, codigoPostalDAO, sessionFactory);
    }

    public boolean isSeedingNecessary() {
        boolean forAyuntamiento = ayuntamientoCollectorService.isSeedingNecessary();
        boolean forCodigoPostal = codigoPostalCollectorService.isSeedingNecessary();
        boolean forBarrio = barrioCollectorService.isSeedingNecessary();
        boolean forCodigoPostalAyuntamientoLinks = codigoPostalToAyuntamientoLinkingService.isSeedingNecessary();

        return forAyuntamiento || forCodigoPostal || forBarrio || forCodigoPostalAyuntamientoLinks;
    }

    public void seed() {
        Stopwatch ayuntamientoCollectorStopwatch = Stopwatch.createStarted();
        ayuntamientoCollectorService.seed();
        ayuntamientoCollectorStopwatch.stop();

        Stopwatch codigoPostalCollectorStopwatch = Stopwatch.createStarted();
        codigoPostalCollectorService.seed();
        codigoPostalCollectorStopwatch.stop();

        Stopwatch barrioCollectorStopwatch = Stopwatch.createStarted();
        barrioCollectorService.seed();
        barrioCollectorStopwatch.stop();

        Stopwatch codigoPostalLinkingStopwatch = Stopwatch.createStarted();
        codigoPostalToAyuntamientoLinkingService.seed();
        codigoPostalLinkingStopwatch.stop();

        LOGGER.info("Finished administrative unit collection with second durations: " +
                        "ayuntamientos {}, codigos postales {}, barrios {}, codigo postal linking {}",
                ayuntamientoCollectorStopwatch.elapsed(TimeUnit.SECONDS),
                codigoPostalCollectorStopwatch.elapsed(TimeUnit.SECONDS),
                barrioCollectorStopwatch.elapsed(TimeUnit.SECONDS),
                codigoPostalLinkingStopwatch.elapsed(TimeUnit.SECONDS));
    }
}

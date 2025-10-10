package com.acantilado.gathering.administration;

import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.BarrioDAO;
import com.acantilado.core.administrative.CodigoPostalDAO;
import com.acantilado.gathering.administration.ayuntamiento.AyuntamientoCollectorService;
import com.acantilado.gathering.administration.barrio.BarrioCollectorService;
import com.acantilado.gathering.administration.codigopostal.CodigoPostalCollectorService;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdministrativeCollectorService extends CollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdministrativeCollectorService.class);

    private final CodigoPostalCollectorService codigoPostalCollectorService;
    private final AyuntamientoCollectorService ayuntamientoCollectorService;
    private final BarrioCollectorService barrioCollectorService;

    public AdministrativeCollectorService(CodigoPostalDAO codigoPostalDAO, AyuntamientoDAO ayuntamientoDAO, BarrioDAO barrioDAO, SessionFactory sessionFactory) {
        this.codigoPostalCollectorService = new CodigoPostalCollectorService(codigoPostalDAO, sessionFactory);
        this.ayuntamientoCollectorService = new AyuntamientoCollectorService(ayuntamientoDAO, sessionFactory);
        this.barrioCollectorService = new BarrioCollectorService(barrioDAO, sessionFactory);
    }

    public boolean isSeedingNecessary() {
        boolean forAyuntamiento = ayuntamientoCollectorService.isSeedingNecessary();
        boolean forCodigoPostal = codigoPostalCollectorService.isSeedingNecessary();
        boolean forBarrio = barrioCollectorService.isSeedingNecessary();

        return forAyuntamiento || forCodigoPostal || forBarrio;
    }

    public void seed() {
        ayuntamientoCollectorService.seed();
        codigoPostalCollectorService.seed();
        barrioCollectorService.seed();
        LOGGER.info("Finished seeding individual administrative units");

        // Link codigos postales to ayuntamientos
        // DONE Link barrios to ayuntamientos
    }
}

package com.acantilado.collection.administration;

import com.acantilado.collection.administration.admin.AdministrativeCollectorService;
import com.acantilado.collection.administration.barrio.BarrioCollectorService;
import com.acantilado.collection.administration.codigopostal.CodigoPostalCollectorService;
import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.BarrioDAO;
import com.acantilado.core.administrative.CodigoPostalDAO;
import com.acantilado.core.administrative.ProvinciaDAO;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeographicCollectorService extends CollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeographicCollectorService.class);

    private final CodigoPostalCollectorService codigoPostalCollectorService;
    private final AdministrativeCollectorService administrativeCollectorService;
    private final BarrioCollectorService barrioCollectorService;
    private final CodigoPostalToAyuntamientoLinkingService codigoPostalToAyuntamientoLinkingService;

    public GeographicCollectorService(CodigoPostalDAO codigoPostalDAO, AyuntamientoDAO ayuntamientoDAO, ProvinciaDAO provinciaDAO, BarrioDAO barrioDAO, SessionFactory sessionFactory) {
        this.codigoPostalCollectorService = new CodigoPostalCollectorService(codigoPostalDAO, sessionFactory);
        this.administrativeCollectorService = new AdministrativeCollectorService(provinciaDAO, ayuntamientoDAO, sessionFactory);
        this.barrioCollectorService = new BarrioCollectorService(barrioDAO, sessionFactory);

        this.codigoPostalToAyuntamientoLinkingService =
                new CodigoPostalToAyuntamientoLinkingService(ayuntamientoDAO, codigoPostalDAO, sessionFactory);
    }

    public boolean isSeedingNecessary() {
        boolean forAyuntamiento = administrativeCollectorService.isSeedingNecessary();
        boolean forCodigoPostal = codigoPostalCollectorService.isSeedingNecessary();
        boolean forBarrio = barrioCollectorService.isSeedingNecessary();
        boolean forCodigoPostalAyuntamientoLinks = codigoPostalToAyuntamientoLinkingService.isSeedingNecessary();

        return forAyuntamiento || forCodigoPostal || forBarrio || forCodigoPostalAyuntamientoLinks;
    }

    public void seed() {
        administrativeCollectorService.seed();
        codigoPostalCollectorService.seed();
        barrioCollectorService.seed();
        codigoPostalToAyuntamientoLinkingService.seed();

        LOGGER.info("Finished administrative unit collection with second durations");
    }
}

package com.acantilado.gathering.properties;

import com.acantilado.core.administrative.*;
import com.acantilado.core.idealista.IdealistaContactInformationDAO;
import com.acantilado.core.idealista.IdealistaLocationDAO;
import com.acantilado.core.idealista.IdealistaPropertyDAO;
import com.acantilado.core.idealista.IdealistaTerrainDAO;
import org.hibernate.SessionFactory;

public class IdealistaCollectorServiceFactory {
    private final ProvinciaDAO provinciaDAO;
    private final AyuntamientoDAO ayuntamientoDAO;
    private final IdealistaLocationMappingDAO mappingDAO;
    private final IdealistaLocationDAO locationDAO;
    private final BarrioDAO barrioDAO;
    private final IdealistaTerrainDAO terrainDAO;
    private final IdealistaContactInformationDAO contactInformationDAO;
    private final IdealistaPropertyDAO propertyDAO;
    private final CodigoPostalDAO codigoPostalDAO;

    private final SessionFactory sessionFactory;

    public IdealistaCollectorServiceFactory(
            IdealistaContactInformationDAO contactDAO,
            IdealistaPropertyDAO propertyDAO,
            IdealistaTerrainDAO terrainDAO,
            IdealistaLocationDAO locationDAO,
            ProvinciaDAO provinciaDAO,
            CodigoPostalDAO codigoPostalDAO,
            AyuntamientoDAO ayuntamientoDAO,
            BarrioDAO barrioDAO,
            IdealistaLocationMappingDAO mappingDAO,
            SessionFactory sessionFactory) {

        this.contactInformationDAO = contactDAO;
        this.propertyDAO = propertyDAO;
        this.terrainDAO = terrainDAO;
        this.codigoPostalDAO = codigoPostalDAO;
        this.provinciaDAO = provinciaDAO;
        this.ayuntamientoDAO = ayuntamientoDAO;
        this.mappingDAO = mappingDAO;
        this.locationDAO = locationDAO;
        this.barrioDAO = barrioDAO;

        this.sessionFactory = sessionFactory;
    }

    public IdealistaProvinceCollectorService getCollectorService(String provinceToCollectFor) {
        return new IdealistaProvinceCollectorService(
                provinceToCollectFor,
                contactInformationDAO,
                propertyDAO,
                terrainDAO,
                locationDAO,
                provinciaDAO,
                codigoPostalDAO,
                ayuntamientoDAO,
                barrioDAO,
                mappingDAO,
                sessionFactory);
    }
}

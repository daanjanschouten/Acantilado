package com.acantilado.collection.amenity;

import com.acantilado.core.administrative.*;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import org.hibernate.SessionFactory;

public final class AmenityCollectorServiceFactory {
    private final GoogleAmenityDAO amenityDAO;
    private final GoogleAmenitySnapshotDAO snapshotDAO;
    private final ProvinciaDAO provinciaDAO;
    private final CodigoPostalDAO codigoPostalDAO;
    private final AyuntamientoDAO ayuntamientoDAO;
    private final BarrioDAO barrioDAO;
    private final IdealistaLocationMappingDAO mappingDAO;
    private final SessionFactory sessionFactory;

    public AmenityCollectorServiceFactory(
            GoogleAmenityDAO amenityDAO,
            GoogleAmenitySnapshotDAO snapshotDAO,
            ProvinciaDAO provinciaDAO,
            CodigoPostalDAO codigoPostalDAO,
            AyuntamientoDAO ayuntamientoDAO,
            BarrioDAO barrioDAO,
            IdealistaLocationMappingDAO mappingDAO,
            SessionFactory sessionFactory) {
        this.amenityDAO = amenityDAO;
        this.snapshotDAO = snapshotDAO;
        this.provinciaDAO = provinciaDAO;
        this.codigoPostalDAO = codigoPostalDAO;
        this.ayuntamientoDAO = ayuntamientoDAO;
        this.barrioDAO = barrioDAO;
        this.mappingDAO = mappingDAO;
        this.sessionFactory = sessionFactory;
    }

    public AmenityProvinceCollectorService getCollectorService(String provinceToCollectFor) {
        return new AmenityProvinceCollectorService(
                provinceToCollectFor,
                amenityDAO,
                snapshotDAO,
                sessionFactory,
                provinciaDAO,
                codigoPostalDAO,
                ayuntamientoDAO,
                barrioDAO,
                mappingDAO);
    }
}
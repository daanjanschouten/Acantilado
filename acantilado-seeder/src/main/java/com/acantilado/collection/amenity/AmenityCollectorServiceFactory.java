package com.acantilado.collection.amenity;

import com.acantilado.core.administrative.AyuntamientoDAO;
import com.acantilado.core.administrative.ProvinciaDAO;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import org.hibernate.SessionFactory;

public class AmenityCollectorServiceFactory {
    private final GoogleAmenityDAO amenityDAO;
    private final GoogleAmenitySnapshotDAO snapshotDAO;
    private final ProvinciaDAO provinciaDAO;
    private final AyuntamientoDAO ayuntamientoDAO;
    private final SessionFactory sessionFactory;

    public AmenityCollectorServiceFactory(
            GoogleAmenityDAO amenityDAO,
            GoogleAmenitySnapshotDAO snapshotDAO,
            ProvinciaDAO provinciaDAO,
            AyuntamientoDAO ayuntamientoDAO,
            SessionFactory sessionFactory) {
        this.amenityDAO = amenityDAO;
        this.snapshotDAO = snapshotDAO;
        this.provinciaDAO = provinciaDAO;
        this.ayuntamientoDAO = ayuntamientoDAO;
        this.sessionFactory = sessionFactory;
    }

    public AmenityProvinceCollectorService getCollectorService(String provinceToCollectFor) {
        return new AmenityProvinceCollectorService(
                provinceToCollectFor,
                amenityDAO,
                snapshotDAO,
                sessionFactory,
                provinciaDAO,
                ayuntamientoDAO);
    }
}
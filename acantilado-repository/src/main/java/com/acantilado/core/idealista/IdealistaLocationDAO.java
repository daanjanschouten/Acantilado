package com.acantilado.core.idealista;

import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class IdealistaLocationDAO extends AbstractDAO<IdealistaAyuntamientoLocation> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaAyuntamientoLocation.class);
    private final SessionFactory sessionFactory;

    public IdealistaLocationDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
        this.sessionFactory = sessionFactory;
    }

    public Optional<IdealistaAyuntamientoLocation> findByLocationId(String locationId) {
        return Optional.ofNullable(get(locationId));
    }

//    public List<IdealistaAyuntamientoLocation> findByProvinceString(String provinceId) {
//        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaAyuntamientoLocation.findByProvinciaId")
//                .setParameter("provinciaId", provinceId)
//                .getResultList();
//    }

    public List<IdealistaAyuntamientoLocation> findByProvinceId(Long provinceId) {
        String provinceString = String.join("-", "0-EU-ES", provinceId.toString());
        LOGGER.info("Constructed province string {} for ID {}", provinceString, provinceId);

        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaAyuntamientoLocation.findByProvinciaId")
                .setParameter("provinciaId", provinceString)
                .getResultList();
    }

    public IdealistaAyuntamientoLocation saveOrUpdate(IdealistaAyuntamientoLocation location) {
        currentSession().saveOrUpdate(location);
        return location;
    }
}
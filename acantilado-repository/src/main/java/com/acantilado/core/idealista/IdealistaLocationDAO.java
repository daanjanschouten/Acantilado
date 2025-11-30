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

    public IdealistaLocationDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<IdealistaAyuntamientoLocation> findByLocationId(String locationId) {
        return Optional.ofNullable(get(locationId));
    }

    public IdealistaAyuntamientoLocation merge(IdealistaAyuntamientoLocation location) {
        return (IdealistaAyuntamientoLocation) currentSession().merge(location);
    }

//    public List<IdealistaAyuntamientoLocation> findByProvinceString(String provinceId) {
//        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaAyuntamientoLocation.findByProvinciaId")
//                .setParameter("provinciaId", provinceId)
//                .getResultList();
//    }

    public List<IdealistaAyuntamientoLocation> findByProvinceId(String provinceId) {
        String provinceString = String.join("-", "0-EU-ES", provinceId);

        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaAyuntamientoLocation.findByProvinciaId")
                .setParameter("provinciaId", provinceString)
                .getResultList();
    }
}
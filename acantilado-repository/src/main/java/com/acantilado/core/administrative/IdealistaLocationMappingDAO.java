package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;

public class IdealistaLocationMappingDAO extends AbstractDAO<IdealistaLocationMapping> {

    public IdealistaLocationMappingDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<IdealistaLocationMapping> findByIdealistaLocationId(String idealistaLocationId) {
        return list(namedTypedQuery("com.acantilado.core.administrative.IdealistaLocationMapping.findByIdealistaLocationId")
                    .setParameter("idealistaLocationId", idealistaLocationId));
    }

    public List<IdealistaLocationMapping> findByAyuntamientoId(String ayuntamientoId) {
        return list(namedTypedQuery("com.acantilado.core.administrative.IdealistaLocationMapping.findByAyuntamientoId")
                .setParameter("ayuntamientoId", ayuntamientoId));
    }

    public List<IdealistaLocationMapping> findByIdealistaMunicipalityName(String municipalityName) {
        return list(namedTypedQuery("com.acantilado.core.administrative.IdealistaLocationMapping.findByIdealistaMunicipalityName")
                .setParameter("municipalityName", municipalityName));
    }

    public List<IdealistaLocationMapping> findAll() {
        return list(namedTypedQuery("com.acantilado.core.administrative.IdealistaLocationMapping.findAll"));
    }

    public IdealistaLocationMapping merge(IdealistaLocationMapping mapping) {
        return (IdealistaLocationMapping) currentSession().merge(mapping);
    }

    public IdealistaLocationMapping create(IdealistaLocationMapping mapping) {
        return persist(mapping);
    }

    public void delete(IdealistaLocationMapping mapping) {
        currentSession().delete(mapping);
    }
}
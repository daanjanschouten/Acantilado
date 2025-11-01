package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import javax.persistence.NoResultException;
import java.util.List;
import java.util.Optional;

public class IdealistaLocationMappingDAO extends AbstractDAO<IdealistaLocationMapping> {

    public IdealistaLocationMappingDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<IdealistaLocationMapping> findByIdealistaLocationId(String idealistaLocationId) {
        try {
            return Optional.of(namedTypedQuery("com.acantilado.core.administrative.IdealistaLocationMapping.findByIdealistaLocationId")
                    .setParameter("idealistaLocationId", idealistaLocationId)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<IdealistaLocationMapping> findByAyuntamientoId(Long ayuntamientoId) {
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

    public IdealistaLocationMapping saveOrUpdate(IdealistaLocationMapping mapping) {
        return (IdealistaLocationMapping) currentSession().merge(mapping);
    }

    public IdealistaLocationMapping create(IdealistaLocationMapping mapping) {
        return persist(mapping);
    }

    public void delete(IdealistaLocationMapping mapping) {
        currentSession().delete(mapping);
    }
}
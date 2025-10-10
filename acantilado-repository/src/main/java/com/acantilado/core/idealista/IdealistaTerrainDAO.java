package com.acantilado.core.idealista;

import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

public class IdealistaTerrainDAO extends IdealistaRealEstateDAO<IdealistaTerrain> {
    public IdealistaTerrainDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<IdealistaTerrain> findByPropertyCode(Long propertyCode) {
        return Optional.ofNullable(get(propertyCode));
    }

    @Override
    public List<IdealistaTerrain> findAll() {
        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaTerrain.findAll")
                .getResultList();
    }

    @Override
    public List<IdealistaTerrain> findByMunicipality(String municipality) {
        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaTerrain.findByMunicipality")
                .setParameter("municipality", municipality)
                .getResultList();
    }

    public List<IdealistaTerrain> findByPropertyType(String propertyType) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<IdealistaTerrain> criteria = builder.createQuery(IdealistaTerrain.class);
        Root<IdealistaTerrain> root = criteria.from(IdealistaTerrain.class);

        criteria.select(root).where(builder.equal(root.get("propertyType"), propertyType));

        return currentSession().createQuery(criteria).getResultList();
    }

    @Override
    public List<IdealistaTerrain> findByAyuntamientoIdIsNull() {
        Query<IdealistaTerrain> query = currentSession().createQuery(
                "SELECT p FROM IdealistaTerrain p WHERE p.ayuntamientoId IS NULL",
                IdealistaTerrain.class);
        return query.getResultList();
    }

    @Override
    public IdealistaTerrain create(IdealistaTerrain property) {
        return persist(property);
    }

    @Override
    public IdealistaTerrain saveOrUpdate(IdealistaTerrain property) {
        currentSession().saveOrUpdate(property);
        return property;
    }

    @Override
    public void delete(IdealistaTerrain property) {
        currentSession().delete(property);
    }

    @Override
    public void deleteByPropertyCode(Long propertyCode) {
        Query<?> query = currentSession().createQuery(
                "DELETE FROM IdealistaTerrain p WHERE p.propertyCode = :propertyCode");
        query.setParameter("propertyCode", propertyCode);
        query.executeUpdate();
    }
}
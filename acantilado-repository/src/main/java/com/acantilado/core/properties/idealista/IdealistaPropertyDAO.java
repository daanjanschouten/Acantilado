package com.acantilado.core.properties.idealista;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

public class IdealistaPropertyDAO extends AbstractDAO<IdealistaProperty> {
    public IdealistaPropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<IdealistaProperty> findByPropertyCode(Long propertyCode) {
        return Optional.ofNullable(get(propertyCode));
    }

    public List<IdealistaProperty> findAll() {
        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaProperty.findAll")
                .getResultList();
    }

    public List<IdealistaProperty> findByMunicipality(String municipality) {
        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaProperty.findByMunicipality")
                .setParameter("municipality", municipality)
                .getResultList();
    }

    public List<IdealistaProperty> findByAyuntamientoId(Long ayuntamientoId) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<IdealistaProperty> criteria = builder.createQuery(IdealistaProperty.class);
        Root<IdealistaProperty> root = criteria.from(IdealistaProperty.class);

        criteria.select(root).where(builder.equal(root.get("ayuntamientoId"), ayuntamientoId));

        return currentSession().createQuery(criteria).getResultList();
    }

    public List<IdealistaProperty> findByPropertyType(String propertyType) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<IdealistaProperty> criteria = builder.createQuery(IdealistaProperty.class);
        Root<IdealistaProperty> root = criteria.from(IdealistaProperty.class);

        criteria.select(root).where(builder.equal(root.get("propertyType"), propertyType));

        return currentSession().createQuery(criteria).getResultList();
    }

    public List<IdealistaProperty> findByAyuntamientoIdIsNull() {
        Query<IdealistaProperty> query = currentSession().createQuery(
                "SELECT p FROM IdealistaProperty p WHERE p.ayuntamientoId IS NULL",
                IdealistaProperty.class);
        return query.getResultList();
    }

    public IdealistaProperty create(IdealistaProperty property) {
        return persist(property);
    }

    public IdealistaProperty saveOrUpdate(IdealistaProperty property) {
        currentSession().saveOrUpdate(property);
        return property;
    }

    public void delete(IdealistaProperty property) {
        currentSession().delete(property);
    }

    public void deleteByPropertyCode(Long propertyCode) {
        Query<?> query = currentSession().createQuery(
                "DELETE FROM IdealistaProperty p WHERE p.propertyCode = :propertyCode");
        query.setParameter("propertyCode", propertyCode);
        query.executeUpdate();
    }
}
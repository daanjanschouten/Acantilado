package com.acantilado.core.idealista;

import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

public class IdealistaPropertyDAO extends IdealistaRealEstateDAO<IdealistaProperty> {
  public IdealistaPropertyDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @Override
  public Optional<IdealistaProperty> findByPropertyCode(Long propertyCode) {
    return Optional.ofNullable(get(propertyCode));
  }

  @Override
  public List<IdealistaProperty> findAll() {
    return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaProperty.findAll")
        .getResultList();
  }

  @Override
  public List<IdealistaProperty> findByMunicipality(String municipality) {
    return namedTypedQuery(
            "com.schouten.core.properties.idealista.IdealistaProperty.findByMunicipality")
        .setParameter("municipality", municipality)
        .getResultList();
  }

  public List<IdealistaProperty> findByPropertyType(String propertyType) {
    CriteriaBuilder builder = currentSession().getCriteriaBuilder();
    CriteriaQuery<IdealistaProperty> criteria = builder.createQuery(IdealistaProperty.class);
    Root<IdealistaProperty> root = criteria.from(IdealistaProperty.class);

    criteria.select(root).where(builder.equal(root.get("propertyType"), propertyType));

    return currentSession().createQuery(criteria).getResultList();
  }

  @Override
  public List<IdealistaProperty> findByAyuntamientoIdIsNull() {
    Query<IdealistaProperty> query =
        currentSession()
            .createQuery(
                "SELECT p FROM IdealistaProperty p WHERE p.ayuntamientoId IS NULL",
                IdealistaProperty.class);
    return query.getResultList();
  }

  @Override
  public IdealistaProperty create(IdealistaProperty property) {
    return persist(property);
  }

  @Override
  public IdealistaProperty merge(IdealistaProperty property) {
    currentSession().merge(property);
    return property;
  }

  @Override
  public void delete(IdealistaProperty property) {
    currentSession().delete(property);
  }

  @Override
  public void deleteByPropertyCode(Long propertyCode) {
    Query<?> query =
        currentSession()
            .createQuery("DELETE FROM IdealistaProperty p WHERE p.propertyCode = :propertyCode");
    query.setParameter("propertyCode", propertyCode);
    query.executeUpdate();
  }
}

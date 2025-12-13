package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;

public class CodigoPostalDAO extends AbstractDAO<CodigoPostal> {

  public CodigoPostalDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  public CodigoPostal create(CodigoPostal codigoPostal) {
    return persist(codigoPostal);
  }

  public Optional<CodigoPostal> findById(String codigoPostal) {
    return Optional.ofNullable(get(codigoPostal));
  }

  public List<CodigoPostal> findAll() {
    return list(namedTypedQuery("com.acantilado.codigopostal.findAll"));
  }

  public List<CodigoPostal> findByAyuntamiento(String ayuntamientoId) {
    return list(
        namedTypedQuery("com.acantilado.codigopostal.findByAyuntamiento")
            .setParameter("ayuntamientoId", ayuntamientoId));
  }

  public List<CodigoPostal> findByCodigoPostal(String codigoPostal) {
    return list(
        namedTypedQuery("com.acantilado.codigopostal.findByCodigoPostal")
            .setParameter("codigo_postal", codigoPostal));
  }

  public List<String> findAllIds() {
    return currentSession()
        .createQuery("SELECT c.codigoIne FROM CodigoPostal c", String.class)
        .getResultList();
  }
}

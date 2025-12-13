package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;

public class AyuntamientoDAO extends AbstractDAO<Ayuntamiento> {
  public AyuntamientoDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  public Optional<Ayuntamiento> findById(String ayuntamientoId) {
    return Optional.ofNullable(get(ayuntamientoId));
  }

  public Ayuntamiento create(Ayuntamiento ayuntamiento) {
    return merge(ayuntamiento);
  }

  private Ayuntamiento merge(Ayuntamiento ayuntamiento) {
    return currentSession().merge(ayuntamiento);
  }

  public List<Ayuntamiento> findAll() {
    return list(namedTypedQuery("com.acantilado.ayuntamiento.findAll"));
  }

  public List<Ayuntamiento> findByProvinceId(String provinceId) {
    return namedTypedQuery("com.acantilado.ayuntamiento.findByProvinceId")
        .setParameter("provinciaId", provinceId)
        .getResultList();
  }

  public List<Ayuntamiento> findByName(String name) {
    return namedTypedQuery("com.acantilado.ayuntamiento.findByName")
        .setParameter("name", name)
        .getResultList();
  }

  public List<String> findAllIds() {
    return currentSession()
        .createQuery("SELECT a.ayuntamientoId FROM Ayuntamiento a", String.class)
        .getResultList();
  }
}
